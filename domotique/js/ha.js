/* Connexion à Home Assistant via son API WebSocket officielle.
   Doc : https://developers.home-assistant.io/docs/api/websocket */

class HABackend {
  constructor(url, token) {
    this.kind = 'ha';
    this.url = url.trim().replace(/\/+$/, '');
    this.token = token.trim();
    this.id = 1;
    this.pending = new Map();
    this.closed = false;
    this.retry = 0;
  }

  get wsUrl() {
    return this.url.replace(/^http/i, 'ws') + '/api/websocket';
  }

  connect(h) {
    this.h = h;
    this._open();
  }

  disconnect() {
    this.closed = true;
    clearTimeout(this.reconnectTimer);
    clearInterval(this.pingTimer);
    try { this.ws?.close(); } catch { /* ignore */ }
  }

  _open() {
    this.h.onStatus('connecting', this.retry ? 'Reconnexion…' : 'Connexion…');
    let ws;
    try {
      ws = new WebSocket(this.wsUrl);
    } catch (err) {
      this.h.onStatus('error', 'Adresse invalide');
      this.h.onError?.(`Adresse invalide : ${err.message}`);
      return;
    }
    this.ws = ws;
    ws.onmessage = ev => this._onMessage(JSON.parse(ev.data));
    ws.onclose = () => {
      clearInterval(this.pingTimer);
      for (const p of this.pending.values()) p.reject(new Error('Connexion perdue'));
      this.pending.clear();
      if (this.closed) return;
      if (this.authFailed) { this.h.onStatus('error', 'Jeton refusé'); return; }
      const delay = Math.min(30000, 1000 * 2 ** this.retry++);
      this.h.onStatus('connecting', `Hors ligne · nouvel essai dans ${Math.round(delay / 1000)} s`);
      this.reconnectTimer = setTimeout(() => this._open(), delay);
    };
  }

  _onMessage(msg) {
    switch (msg.type) {
      case 'auth_required':
        this.ws.send(JSON.stringify({ type: 'auth', access_token: this.token }));
        break;
      case 'auth_invalid':
        this.authFailed = true;
        this.h.onStatus('error', 'Jeton refusé');
        this.h.onError?.('Home Assistant a refusé le jeton d’accès. Vérifie-le dans les réglages.');
        try { this.ws.close(); } catch { /* ignore */ }
        break;
      case 'auth_ok':
        this.retry = 0;
        this._init().catch(err => this.h.onError?.(`Chargement impossible : ${err.message}`));
        break;
      case 'result': {
        const p = this.pending.get(msg.id);
        if (!p) return;
        this.pending.delete(msg.id);
        if (msg.success) p.resolve(msg.result);
        else p.reject(new Error(msg.error?.message || 'Erreur Home Assistant'));
        break;
      }
      case 'pong': {
        const p = this.pending.get(msg.id);
        if (p) { this.pending.delete(msg.id); p.resolve(); }
        break;
      }
      case 'event': {
        const data = msg.event?.data;
        if (msg.event?.event_type === 'state_changed' && data) this.h.onState(data.entity_id, data.new_state);
        break;
      }
    }
  }

  _send(payload, timeout = 15000) {
    return new Promise((resolve, reject) => {
      if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return reject(new Error('Non connecté à Home Assistant'));
      const id = this.id++;
      const timer = setTimeout(() => { this.pending.delete(id); reject(new Error('Pas de réponse de Home Assistant')); }, timeout);
      this.pending.set(id, {
        resolve: v => { clearTimeout(timer); resolve(v); },
        reject: e => { clearTimeout(timer); reject(e); },
      });
      this.ws.send(JSON.stringify({ id, ...payload }));
    });
  }

  async _init() {
    // Les registres (pièces, appareils) demandent un compte administrateur :
    // sans eux, tout s'affiche quand même, regroupé dans « Autres ».
    const soft = p => p.catch(() => []);
    const [states, areas, entities, devices] = await Promise.all([
      this._send({ type: 'get_states' }),
      soft(this._send({ type: 'config/area_registry/list' })),
      soft(this._send({ type: 'config/entity_registry/list' })),
      soft(this._send({ type: 'config/device_registry/list' })),
    ]);
    const deviceArea = Object.fromEntries(devices.map(d => [d.id, d.area_id]));
    const areaOf = {}, registry = {};
    for (const r of entities) {
      registry[r.entity_id] = r;
      const a = r.area_id || deviceArea[r.device_id];
      if (a) areaOf[r.entity_id] = a;
    }
    await this._send({ type: 'subscribe_events', event_type: 'state_changed' });
    this.h.onInit({ states, areas: areas.map(a => ({ area_id: a.area_id, name: a.name })), areaOf, registry });
    this.h.onStatus('online', 'Connecté');
    clearInterval(this.pingTimer);
    this.pingTimer = setInterval(() => {
      this._send({ type: 'ping' }, 10000).catch(() => { try { this.ws.close(); } catch { /* ignore */ } });
    }, 30000);
  }

  /* Réglages du tableau de bord Énergie de HA (compteur, appareils suivis). */
  energyPrefs() {
    return this._send({ type: 'energy/get_prefs' }).catch(() => null);
  }

  /* Statistiques long terme (kWh consommés par heure ou par jour). */
  statistics(ids, start, end, period) {
    return this._send({
      type: 'recorder/statistics_during_period',
      start_time: new Date(start).toISOString(),
      end_time: new Date(end).toISOString(),
      statistic_ids: ids,
      period,
      types: ['change', 'sum'],
      units: { energy: 'kWh' },
    }, 30000);
  }

  /* Historique brut d'un capteur (courbe de puissance). */
  history(ids, start, end) {
    return this._send({
      type: 'history/history_during_period',
      start_time: new Date(start).toISOString(),
      end_time: new Date(end).toISOString(),
      entity_ids: ids,
      minimal_response: true,
      no_attributes: true,
      significant_changes_only: false,
    }, 30000);
  }

  call(domain, service, data = {}, entityId) {
    const msg = { type: 'call_service', domain, service, service_data: data };
    if (entityId) msg.target = { entity_id: entityId };
    return this._send(msg);
  }
}
