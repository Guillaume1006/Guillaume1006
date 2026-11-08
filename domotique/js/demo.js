/* Maison de démonstration : simule Home Assistant (mêmes entités, mêmes services)
   pour essayer l'application sans rien installer. L'état est gardé dans le navigateur. */

const DEMO_KEY = 'domo.demo.v1';

const DEMO_AREAS = [
  { area_id: 'salon', name: 'Salon' },
  { area_id: 'cuisine', name: 'Cuisine' },
  { area_id: 'chambre', name: 'Chambre' },
  { area_id: 'bureau', name: 'Bureau' },
  { area_id: 'entree', name: 'Entrée' },
  { area_id: 'exterieur', name: 'Extérieur' },
];

function demoEntities() {
  const now = new Date().toISOString();
  const L = (id, name, area, on, extra = {}) => ({ id, area, state: on ? 'on' : 'off', attributes: { friendly_name: name, supported_color_modes: ['brightness'], brightness: on ? 200 : null, ...extra } });
  const list = [
    L('light.salon_plafonnier', 'Salon Plafonnier', 'salon', true, { supported_color_modes: ['color_temp', 'hs'], color_mode: 'color_temp', color_temp_kelvin: 3000, min_color_temp_kelvin: 2200, max_color_temp_kelvin: 6500 }),
    L('light.salon_lampadaire', 'Salon Lampadaire', 'salon', false),
    L('light.salon_ruban_led', 'Salon Ruban LED', 'salon', true, { supported_color_modes: ['hs'], color_mode: 'hs', hs_color: [280, 80], rgb_color: [180, 60, 255], brightness: 120 }),
    { id: 'switch.salon_tv', area: 'salon', state: 'off', attributes: { friendly_name: 'Salon Prise TV' } },
    { id: 'cover.salon_volet', area: 'salon', state: 'open', attributes: { friendly_name: 'Salon Volet', current_position: 100 } },
    { id: 'climate.salon_thermostat', area: 'salon', state: 'heat', attributes: { friendly_name: 'Salon Thermostat', current_temperature: 20.4, temperature: 21, min_temp: 7, max_temp: 30, target_temp_step: 0.5, hvac_modes: ['off', 'heat', 'auto'] } },
    { id: 'sensor.salon_temperature', area: 'salon', state: '20.4', attributes: { friendly_name: 'Salon Température', device_class: 'temperature', unit_of_measurement: '°C' } },
    { id: 'sensor.salon_humidite', area: 'salon', state: '48', attributes: { friendly_name: 'Salon Humidité', device_class: 'humidity', unit_of_measurement: '%' } },

    L('light.cuisine_plafonnier', 'Cuisine Plafonnier', 'cuisine', false),
    L('light.cuisine_plan_de_travail', 'Cuisine Plan de travail', 'cuisine', false, { supported_color_modes: ['onoff'], brightness: null }),
    { id: 'switch.cuisine_cafetiere', area: 'cuisine', state: 'off', attributes: { friendly_name: 'Cuisine Cafetière' } },
    { id: 'binary_sensor.cuisine_fenetre', area: 'cuisine', state: 'on', attributes: { friendly_name: 'Cuisine Fenêtre', device_class: 'window' } },
    { id: 'binary_sensor.cuisine_fuite', area: 'cuisine', state: 'off', attributes: { friendly_name: 'Cuisine Détecteur de fuite', device_class: 'moisture' } },
    { id: 'sensor.cuisine_temperature', area: 'cuisine', state: '19.1', attributes: { friendly_name: 'Cuisine Température', device_class: 'temperature', unit_of_measurement: '°C' } },

    L('light.chambre_plafonnier', 'Chambre Plafonnier', 'chambre', false),
    L('light.chambre_chevet', 'Chambre Chevet', 'chambre', false, { supported_color_modes: ['color_temp'], color_mode: 'color_temp', color_temp_kelvin: 2700, min_color_temp_kelvin: 2200, max_color_temp_kelvin: 6500 }),
    { id: 'cover.chambre_volet', area: 'chambre', state: 'closed', attributes: { friendly_name: 'Chambre Volet', current_position: 0 } },
    { id: 'climate.chambre_radiateur', area: 'chambre', state: 'heat', attributes: { friendly_name: 'Chambre Radiateur', current_temperature: 18.2, temperature: 18, min_temp: 7, max_temp: 28, target_temp_step: 0.5, hvac_modes: ['off', 'heat'] } },
    { id: 'fan.chambre_ventilateur', area: 'chambre', state: 'off', attributes: { friendly_name: 'Chambre Ventilateur' } },
    { id: 'sensor.chambre_temperature', area: 'chambre', state: '18.2', attributes: { friendly_name: 'Chambre Température', device_class: 'temperature', unit_of_measurement: '°C' } },

    L('light.bureau_lampe', 'Bureau Lampe', 'bureau', true, { brightness: 255 }),
    { id: 'switch.bureau_ordinateur', area: 'bureau', state: 'on', attributes: { friendly_name: 'Bureau Prise ordinateur' } },
    { id: 'cover.bureau_volet', area: 'bureau', state: 'open', attributes: { friendly_name: 'Bureau Volet', current_position: 60 } },
    { id: 'sensor.bureau_co2', area: 'bureau', state: '780', attributes: { friendly_name: 'Bureau CO₂', device_class: 'carbon_dioxide', unit_of_measurement: 'ppm' } },

    L('light.entree', 'Entrée Lumière', 'entree', false, { supported_color_modes: ['onoff'], brightness: null }),
    { id: 'lock.porte_entree', area: 'entree', state: 'locked', attributes: { friendly_name: 'Porte d’entrée' } },
    { id: 'binary_sensor.entree_porte', area: 'entree', state: 'off', attributes: { friendly_name: 'Entrée Porte', device_class: 'door' } },
    { id: 'binary_sensor.entree_mouvement', area: 'entree', state: 'off', attributes: { friendly_name: 'Entrée Mouvement', device_class: 'motion' } },
    { id: 'sensor.consommation_maison', area: 'entree', state: '640', attributes: { friendly_name: 'Consommation maison', device_class: 'power', unit_of_measurement: 'W' } },

    L('light.jardin', 'Extérieur Jardin', 'exterieur', false, { supported_color_modes: ['onoff'], brightness: null }),
    { id: 'switch.arrosage', area: 'exterieur', state: 'off', attributes: { friendly_name: 'Extérieur Arrosage' } },
    { id: 'cover.garage', area: 'exterieur', state: 'closed', attributes: { friendly_name: 'Extérieur Porte de garage', current_position: 0, device_class: 'garage' } },
    { id: 'sensor.exterieur_temperature', area: 'exterieur', state: '9.6', attributes: { friendly_name: 'Extérieur Température', device_class: 'temperature', unit_of_measurement: '°C' } },
    { id: 'sensor.exterieur_humidite', area: 'exterieur', state: '81', attributes: { friendly_name: 'Extérieur Humidité', device_class: 'humidity', unit_of_measurement: '%' } },

    { id: 'scene.soiree_film', state: 'scening', attributes: { friendly_name: 'Soirée film', icon_name: 'tv' } },
    { id: 'scene.bonne_nuit', state: 'scening', attributes: { friendly_name: 'Bonne nuit', icon_name: 'moon' } },
    { id: 'scene.reveil', state: 'scening', attributes: { friendly_name: 'Réveil', icon_name: 'sun' } },
    { id: 'scene.depart', state: 'scening', attributes: { friendly_name: 'Je pars', icon_name: 'lock' } },
    { id: 'scene.retour', state: 'scening', attributes: { friendly_name: 'Je rentre', icon_name: 'home' } },

    { id: 'automation.volets_coucher_soleil', state: 'on', attributes: { friendly_name: 'Fermer les volets au coucher du soleil', description: 'Ferme tous les volets 15 min après le coucher du soleil.', last_triggered: new Date(Date.now() - 20 * 3600e3).toISOString() } },
    { id: 'automation.lumiere_entree_mouvement', state: 'on', attributes: { friendly_name: 'Lumière de l’entrée sur mouvement', description: 'Allume l’entrée 2 min quand un mouvement est détecté la nuit.', last_triggered: new Date(Date.now() - 3 * 3600e3).toISOString() } },
    { id: 'automation.chauffage_fenetre', state: 'on', attributes: { friendly_name: 'Couper le chauffage si fenêtre ouverte', description: 'Met le thermostat en arrêt quand une fenêtre reste ouverte 5 min.', last_triggered: null } },
    { id: 'automation.arrosage_matin', state: 'off', attributes: { friendly_name: 'Arrosage à 6 h', description: 'Arrose le jardin 20 min chaque matin s’il n’a pas plu.', last_triggered: new Date(Date.now() - 5 * 86400e3).toISOString() } },
  ];
  return list.map(x => ({ entity_id: x.id, area: x.area, state: x.state, attributes: x.attributes, last_changed: now }));
}

/* États cibles appliqués par chaque scène de démonstration. */
const DEMO_SCENES = {
  'scene.soiree_film': [['light', 'turn_off', 'light.salon_plafonnier'], ['light', 'turn_on', 'light.salon_ruban_led', { brightness_pct: 25, hs_color: [230, 80] }], ['light', 'turn_on', 'light.salon_lampadaire', { brightness_pct: 15 }], ['switch', 'turn_on', 'switch.salon_tv'], ['cover', 'close_cover', 'cover.salon_volet']],
  'scene.bonne_nuit': [['light', 'turn_off', '*'], ['cover', 'close_cover', '*'], ['lock', 'lock', 'lock.porte_entree'], ['switch', 'turn_off', 'switch.salon_tv'], ['climate', 'set_temperature', 'climate.chambre_radiateur', { temperature: 17 }]],
  'scene.reveil': [['cover', 'open_cover', 'cover.chambre_volet'], ['light', 'turn_on', 'light.chambre_chevet', { brightness_pct: 60, color_temp_kelvin: 4000 }], ['switch', 'turn_on', 'switch.cuisine_cafetiere'], ['climate', 'set_temperature', 'climate.salon_thermostat', { temperature: 21 }]],
  'scene.depart': [['light', 'turn_off', '*'], ['switch', 'turn_off', 'switch.salon_tv'], ['switch', 'turn_off', 'switch.cuisine_cafetiere'], ['lock', 'lock', 'lock.porte_entree'], ['climate', 'set_temperature', 'climate.salon_thermostat', { temperature: 17 }]],
  'scene.retour': [['lock', 'unlock', 'lock.porte_entree'], ['light', 'turn_on', 'light.entree'], ['light', 'turn_on', 'light.salon_plafonnier', { brightness_pct: 80 }], ['climate', 'set_temperature', 'climate.salon_thermostat', { temperature: 21 }]],
};
const DEMO_AUTOMATIONS = {
  'automation.volets_coucher_soleil': [['cover', 'close_cover', '*']],
  'automation.lumiere_entree_mouvement': [['light', 'turn_on', 'light.entree']],
  'automation.chauffage_fenetre': [['climate', 'set_hvac_mode', 'climate.salon_thermostat', { hvac_mode: 'off' }]],
  'automation.arrosage_matin': [['switch', 'turn_on', 'switch.arrosage']],
};

class DemoBackend {
  constructor() {
    this.kind = 'demo';
    this.entities = {};
    this.timers = [];
    this.coverAnims = {};
  }

  connect(h) {
    this.h = h;
    let saved = null;
    try { saved = JSON.parse(localStorage.getItem(DEMO_KEY) || 'null'); } catch { /* ignore */ }
    const base = demoEntities();
    const areaOf = {};
    for (const e of base) {
      const s = saved?.[e.entity_id];
      if (s) { e.state = s.state; e.attributes = { ...e.attributes, ...s.attributes }; }
      // un volet sauvegardé en plein mouvement repart d'un état stable
      if (e.state === 'opening') e.state = 'open';
      if (e.state === 'closing') e.state = 'closed';
      areaOf[e.entity_id] = e.area;
      delete e.area;
      this.entities[e.entity_id] = e;
    }
    h.onStatus('demo', 'Démo');
    h.onInit({ states: Object.values(this.entities).map(clone), areas: DEMO_AREAS, areaOf });
    this.timers.push(setInterval(() => this._drift(), 8000));
  }

  disconnect() {
    this.timers.forEach(clearInterval);
    Object.values(this.coverAnims).forEach(clearInterval);
    this.timers = []; this.coverAnims = {};
  }

  reset() { localStorage.removeItem(DEMO_KEY); }

  async call(domain, service, data = {}, entityId) {
    await new Promise(r => setTimeout(r, 120)); // petite latence réaliste
    const ids = this._targets(domain, entityId);
    if (!ids.length) throw new Error('Aucun appareil ciblé');
    for (const id of ids) this._apply(domain, service, data, id);
    this._save();
  }

  _targets(domain, entityId) {
    if (entityId === '*' || entityId === 'all') return Object.keys(this.entities).filter(id => domainOf(id) === domain);
    return (Array.isArray(entityId) ? entityId : [entityId]).filter(id => this.entities[id]);
  }

  _set(id, state, attrs = {}) {
    const old = this.entities[id];
    const next = { ...old, state, attributes: { ...old.attributes, ...attrs } };
    if (old.state !== state) next.last_changed = new Date().toISOString();
    this.entities[id] = next;
    this.h.onState(id, clone(next));
  }

  _apply(domain, service, data, id) {
    const e = this.entities[id];
    const d = domainOf(id);
    if (service === 'toggle') {
      if (d === 'cover') service = e.state === 'closed' ? 'open_cover' : 'close_cover';
      else if (d === 'lock') service = e.state === 'locked' ? 'unlock' : 'lock';
      else service = e.state === 'on' ? 'turn_off' : 'turn_on';
    }
    switch (d) {
      case 'light': {
        if (service === 'turn_off') return this._set(id, 'off', { brightness: null });
        const a = {};
        const modes = e.attributes.supported_color_modes || [];
        if (!modes.includes('onoff')) a.brightness = data.brightness_pct != null ? Math.round(data.brightness_pct * 2.55) : (e.attributes.brightness || 200);
        if (data.hs_color) { a.hs_color = data.hs_color; a.color_mode = 'hs'; a.rgb_color = hsToRgb(...data.hs_color); }
        if (data.color_temp_kelvin) { a.color_temp_kelvin = data.color_temp_kelvin; a.color_mode = 'color_temp'; }
        return this._set(id, 'on', a);
      }
      case 'switch': case 'input_boolean': case 'fan': case 'automation':
        if (d === 'automation' && service === 'trigger') {
          for (const [dd, ss, tt, dt] of DEMO_AUTOMATIONS[id] || []) for (const t of this._targets(dd, tt)) this._apply(dd, ss, dt || {}, t);
          return this._set(id, e.state, { last_triggered: new Date().toISOString() });
        }
        return this._set(id, service === 'turn_off' ? 'off' : 'on');
      case 'cover': {
        let target = e.attributes.current_position ?? (e.state === 'closed' ? 0 : 100);
        if (service === 'open_cover') target = 100;
        else if (service === 'close_cover') target = 0;
        else if (service === 'set_cover_position') target = Number(data.position);
        else if (service === 'stop_cover') { this._stopCover(id); return; }
        return this._animateCover(id, target);
      }
      case 'climate':
        if (service === 'set_temperature') return this._set(id, e.state === 'off' ? 'heat' : e.state, { temperature: Number(data.temperature) });
        if (service === 'set_hvac_mode') return this._set(id, data.hvac_mode);
        if (service === 'turn_off') return this._set(id, 'off');
        if (service === 'turn_on') return this._set(id, 'heat');
        return;
      case 'lock':
        return this._set(id, service === 'unlock' ? 'unlocked' : 'locked');
      case 'scene':
        for (const [dd, ss, tt, dt] of DEMO_SCENES[id] || []) for (const t of this._targets(dd, tt)) this._apply(dd, ss, dt || {}, t);
        return this._set(id, 'scening', { last_activated: new Date().toISOString() });
    }
  }

  _animateCover(id, target) {
    this._stopCover(id, true);
    const step = () => {
      const e = this.entities[id];
      const pos = e.attributes.current_position ?? 0;
      if (pos === target) {
        clearInterval(this.coverAnims[id]); delete this.coverAnims[id];
        this._set(id, target === 0 ? 'closed' : 'open', { current_position: target });
        this._save();
        return;
      }
      const next = pos < target ? Math.min(target, pos + 10) : Math.max(target, pos - 10);
      this._set(id, next > pos ? 'opening' : 'closing', { current_position: next });
    };
    this.coverAnims[id] = setInterval(step, 350);
    step();
  }

  _stopCover(id, silent) {
    if (this.coverAnims[id]) { clearInterval(this.coverAnims[id]); delete this.coverAnims[id]; }
    if (!silent) {
      const pos = this.entities[id].attributes.current_position ?? 0;
      this._set(id, pos === 0 ? 'closed' : 'open');
    }
  }

  /* Fait vivre la maison : températures, consommation, mouvements. */
  _drift() {
    const E = this.entities;
    for (const [cid, sid] of [['climate.salon_thermostat', 'sensor.salon_temperature'], ['climate.chambre_radiateur', 'sensor.chambre_temperature']]) {
      const c = E[cid]; const cur = Number(E[sid].state);
      const goal = c.state === 'off' ? Number(E['sensor.exterieur_temperature'].state) + 8 : c.attributes.temperature;
      const next = Math.round((cur + Math.sign(goal - cur) * Math.min(0.1, Math.abs(goal - cur))) * 10) / 10;
      if (next !== cur) { this._set(sid, String(next)); this._set(cid, c.state, { current_temperature: next }); }
    }
    const lightsOn = Object.values(E).filter(e => domainOf(e.entity_id) === 'light' && e.state === 'on').length;
    const switchesOn = Object.values(E).filter(e => domainOf(e.entity_id) === 'switch' && e.state === 'on').length;
    const heating = Object.values(E).filter(e => domainOf(e.entity_id) === 'climate' && e.state !== 'off').length;
    const watts = 180 + lightsOn * 9 + switchesOn * 65 + heating * 220 + Math.round(Math.random() * 40);
    this._set('sensor.consommation_maison', String(watts));
    if (Math.random() < 0.15) {
      this._set('binary_sensor.entree_mouvement', 'on');
      setTimeout(() => this._set('binary_sensor.entree_mouvement', 'off'), 4000);
    }
  }

  _save() {
    const out = {};
    for (const [id, e] of Object.entries(this.entities)) out[id] = { state: e.state, attributes: e.attributes };
    try { localStorage.setItem(DEMO_KEY, JSON.stringify(out)); } catch { /* stockage indisponible */ }
  }
}

function clone(o) { return JSON.parse(JSON.stringify(o)); }

function hsToRgb(h, s, v = 100) {
  s /= 100; v /= 100;
  const f = n => { const k = (n + h / 60) % 6; return Math.round(255 * (v - v * s * Math.max(0, Math.min(k, 4 - k, 1)))); };
  return [f(5), f(3), f(1)];
}
