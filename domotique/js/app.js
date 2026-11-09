/* Ma Maison — interface. Fonctionne avec la maison de démo ou avec Home Assistant. */

const store = {
  get(key, fallback) {
    try { const v = localStorage.getItem(key); return v == null ? fallback : JSON.parse(v); } catch { return fallback; }
  },
  set(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* stockage indisponible */ }
  },
};

const DEFAULT_SETTINGS = {
  mode: 'demo',
  url: location.pathname.startsWith('/local/') ? location.origin : '',
  token: '',
  theme: 'auto',
  price: 0.25,       // €/kWh, pour le coût estimé
  energyMeter: '',   // vide = tableau Énergie de HA ou détection automatique
  powerSensor: '',
};

const S = {
  settings: { ...DEFAULT_SETTINGS, ...store.get('domo.settings', {}) },
  favs: store.get('domo.favs', null),
  log: store.get('domo.log', []),
  entities: {},
  areas: [],
  areaOf: {},
  registry: {},
  status: { kind: 'connecting', text: '' },
  ready: false,
  modalId: null,
  dragging: false,
  pendingTemp: {},
  energy: { period: store.get('domo.energyPeriod', 'day'), token: 0, key: null, at: 0, data: null, error: null, loadingKey: null },
};

let backend = null;
const OTHER_AREA = '_autres';
const DOMAIN_ORDER = ['light', 'switch', 'fan', 'input_boolean', 'cover', 'climate', 'lock', 'binary_sensor', 'sensor'];
const NAV = [
  ['home', 'Accueil', 'home'],
  ['rooms', 'Pièces', 'rooms'],
  ['energy', 'Énergie', 'bolt'],
  ['scenes', 'Scènes', 'sparkle'],
  ['automations', 'Automatismes', 'robot', 'Auto'],
  ['log', 'Journal', 'list', null, true], // sur mobile : accessible depuis Réglages
  ['settings', 'Réglages', 'gear'],
];

/* ---------- Connexion ---------- */

function startBackend() {
  backend?.disconnect();
  Object.assign(S, { entities: {}, areas: [], areaOf: {}, registry: {}, ready: false });
  Object.assign(S.energy, { token: S.energy.token + 1, key: null, at: 0, data: null, error: null, loadingKey: null });
  const { mode, url, token } = S.settings;
  if (mode === 'ha' && (!url || !token)) {
    backend = null;
    setStatus('error', 'À configurer');
    render();
    return;
  }
  backend = mode === 'ha' ? new HABackend(url, token) : new DemoBackend();
  backend.connect({
    onStatus: setStatus,
    onError: msg => toast(msg, 'error'),
    onInit({ states, areas, areaOf, registry }) {
      S.entities = Object.fromEntries(states.map(e => [e.entity_id, e]));
      S.areas = areas;
      S.areaOf = areaOf || {};
      S.registry = registry || {};
      S.ready = true;
      scheduleRender();
    },
    onState(id, next) {
      const old = S.entities[id];
      if (!next) delete S.entities[id];
      else S.entities[id] = next;
      if (old && next && old.state !== next.state) addLog(old, next);
      if (next && S.pendingTemp[id] != null && next.attributes?.temperature === S.pendingTemp[id]) delete S.pendingTemp[id];
      scheduleRender();
    },
  });
}

function setStatus(kind, text) {
  S.status = { kind, text };
  const el = document.getElementById('status');
  el.className = 'status s-' + kind;
  el.innerHTML = `<span class="status-dot"></span>${esc(text)}`;
  scheduleRender();
}

async function act(domain, service, data, entityId, okMessage) {
  if (!backend) { toast('Configure d’abord la connexion dans les réglages.', 'error'); return; }
  try {
    await backend.call(domain, service, data || {}, entityId);
    if (okMessage) toast(okMessage, 'ok');
  } catch (err) {
    toast(err.message || String(err), 'error');
  }
}

/* ---------- Journal ---------- */

function addLog(old, next) {
  const d = domainOf(next.entity_id);
  if (d === 'sensor' || d === 'scene') return; // trop bavard / pas d'état utile
  S.log.unshift({ t: Date.now(), id: next.entity_id, name: nameOf(next), from: fmtState(old), to: fmtState(next), icon: entityIcon(next), alert: isAlert(next) });
  S.log.length = Math.min(S.log.length, 200);
  store.set('domo.log', S.log);
}

/* ---------- Données dérivées ---------- */

const all = () => Object.values(S.entities);
const ofDomain = d => all().filter(e => domainOf(e.entity_id) === d);

function areaName(areaId) {
  if (areaId === OTHER_AREA) return 'Autres';
  return S.areas.find(a => a.area_id === areaId)?.name || areaId;
}

function devices() {
  return all().filter(e => isVisibleDevice(e, S.registry[e.entity_id]));
}

function sortDevices(list) {
  return list.sort((a, b) => DOMAIN_ORDER.indexOf(domainOf(a.entity_id)) - DOMAIN_ORDER.indexOf(domainOf(b.entity_id)) || nameOf(a).localeCompare(nameOf(b), 'fr'));
}

function devicesIn(areaId) {
  const known = new Set(S.areas.map(a => a.area_id));
  return sortDevices(devices().filter(e => {
    const a = S.areaOf[e.entity_id];
    return areaId === OTHER_AREA ? !a || !known.has(a) : a === areaId;
  }));
}

function rooms() {
  const list = S.areas.map(a => a.area_id).filter(id => devicesIn(id).length);
  if (devicesIn(OTHER_AREA).length) list.push(OTHER_AREA);
  return list;
}

const OUTDOOR_RE = /ext[ée]rieur|outdoor|outside|dehors|jardin|garden|terrasse|balcon/i;
function isOutdoor(e) {
  return OUTDOOR_RE.test(e.entity_id) || OUTDOOR_RE.test(nameOf(e)) || OUTDOOR_RE.test(areaName(S.areaOf[e.entity_id] || ''));
}

function temps() {
  const sensors = devices().filter(e => domainOf(e.entity_id) === 'sensor' && e.attributes.device_class === 'temperature' && !isUnavailable(e));
  const inside = sensors.filter(e => !isOutdoor(e)).map(e => Number(e.state));
  let outside = sensors.filter(isOutdoor).map(e => Number(e.state))[0];
  const weather = ofDomain('weather')[0];
  if (outside == null && weather?.attributes?.temperature != null) outside = Number(weather.attributes.temperature);
  return { inside: inside.length ? inside.reduce((a, b) => a + b, 0) / inside.length : null, outside };
}

function roomTemp(areaId) {
  const t = devicesIn(areaId).find(e => domainOf(e.entity_id) === 'sensor' && e.attributes.device_class === 'temperature' && !isUnavailable(e));
  if (t) return Number(t.state);
  const c = devicesIn(areaId).find(e => domainOf(e.entity_id) === 'climate' && e.attributes.current_temperature != null);
  return c ? Number(c.attributes.current_temperature) : null;
}

function mainPower() {
  const p = devices().filter(e => e.attributes.device_class === 'power' && !isUnavailable(e));
  if (!p.length) return null;
  const main = p.find(e => /maison|total|home|consommation|compteur|linky/i.test(e.entity_id + ' ' + nameOf(e))) || p.sort((a, b) => Number(b.state) - Number(a.state))[0];
  const w = Number(main.state), unit = main.attributes.unit_of_measurement;
  return unit === 'kW' ? w * 1000 : w;
}

function favourites() {
  if (S.favs) return S.favs.map(id => S.entities[id]).filter(Boolean);
  // par défaut : quelques appareils pilotables
  const pick = ['light', 'cover', 'climate', 'lock'];
  return sortDevices(devices().filter(e => pick.includes(domainOf(e.entity_id)))).slice(0, 8);
}

/* ---------- Rendu ---------- */

let renderQueued = false;
function scheduleRender() {
  if (renderQueued) return;
  renderQueued = true;
  requestAnimationFrame(() => { renderQueued = false; render(); });
}

function route() {
  const [name, arg] = location.hash.replace(/^#\/?/, '').split('/');
  return { name: NAV.some(n => n[0] === name) || name === 'room' ? name : 'home', arg: arg && decodeURIComponent(arg) };
}

function render() {
  const r = route();
  renderNav(r.name === 'room' ? 'rooms' : r.name);
  const view = document.getElementById('view');
  const scroll = window.scrollY;
  let html;
  if (r.name === 'settings') html = viewSettings();
  else if (!S.ready) html = viewLoading();
  else html = { home: viewHome, rooms: viewRooms, room: () => viewRoom(r.arg), energy: viewEnergy, scenes: viewScenes, automations: viewAutomations, log: viewLog }[r.name]();
  // ne pas écraser un formulaire en cours de saisie
  if (r.name === 'settings' && view.dataset.view === 'settings' && view.contains(document.activeElement)) return renderModal();
  if (view.dataset.html !== html) {
    view.innerHTML = html;
    view.dataset.html = html;
    view.dataset.view = r.name;
    window.scrollTo(0, scroll);
  }
  renderModal();
}

function renderNav(active) {
  document.getElementById('nav').innerHTML = NAV.map(([id, label, ic, short, deskOnly]) =>
    `<a href="#/${id}" class="nav-item${id === active ? ' active' : ''}${deskOnly ? ' desk-only' : ''}"${id === active ? ' aria-current="page"' : ''}>${icon(ic)}<span${short ? ` data-short="${short}"` : ''}>${label}</span></a>`).join('');
}

function viewLoading() {
  const s = S.status;
  if (S.settings.mode === 'ha' && (s.kind === 'error' || !backend)) {
    return `<div class="empty big">${icon('alert')}<h2>Connexion à Home Assistant impossible</h2>
      <p>${esc(s.text || 'Réglages incomplets')}. Vérifie l’adresse et le jeton d’accès.</p>
      <p><a class="btn primary" href="#/settings">Ouvrir les réglages</a> <button class="btn" data-act="use-demo">Essayer la démo</button></p></div>`;
  }
  return `<div class="empty big"><div class="spinner"></div><p>${esc(s.text || 'Chargement…')}</p></div>`;
}

function greeting() {
  const h = new Date().getHours();
  return h < 6 ? 'Bonne nuit' : h < 12 ? 'Bonjour' : h < 18 ? 'Bon après-midi' : 'Bonsoir';
}

function viewHome() {
  const lightsOn = ofDomain('light').filter(e => e.state === 'on' && isVisibleDevice(e, S.registry[e.entity_id]));
  const t = temps();
  const power = mainPower();
  const locks = devices().filter(e => domainOf(e.entity_id) === 'lock');
  const alerts = devices().filter(isAlert);
  const scenes = [...ofDomain('scene'), ...ofDomain('script')];
  const favs = favourites();
  const date = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });

  const chips = [];
  if (t.inside != null) chips.push(`<div class="stat"><span class="stat-ic warm">${icon('thermo')}</span><span><b>${fmtNum(t.inside)} °C</b><small>Intérieur</small></span></div>`);
  if (t.outside != null) chips.push(`<div class="stat"><span class="stat-ic cool">${icon('sun')}</span><span><b>${fmtNum(t.outside)} °C</b><small>Extérieur</small></span></div>`);
  if (power != null) chips.push(`<a class="stat" href="#/energy"><span class="stat-ic power">${icon('bolt')}</span><span><b>${power >= 1000 ? fmtNum(power / 1000) + ' kW' : fmtNum(power, 0) + ' W'}</b><small>Consommation · graphiques</small></span></a>`);
  const lightsTotal = ofDomain('light').filter(e => isVisibleDevice(e, S.registry[e.entity_id])).length;
  chips.push(`<div class="stat"><span class="stat-ic ${lightsOn.length ? 'light' : ''}">${icon('bulb')}</span><span><b>${lightsOn.length ? `${lightsOn.length} / ${lightsTotal}` : 'Éteintes'}</b><small>Lumières</small></span>${lightsOn.length ? `<button class="icon-btn small" data-act="all-off" aria-label="Éteindre toutes les lumières" title="Tout éteindre">${icon('power')}</button>` : ''}</div>`);
  // la serrure ouvre son détail plutôt que de basculer : pas de déverrouillage par erreur
  for (const l of locks) {
    const locked = l.state === 'locked';
    chips.push(`<button class="stat" data-act="open" data-id="${esc(l.entity_id)}"><span class="stat-ic ${locked ? 'ok' : 'bad'}">${icon(locked ? 'lock' : 'unlock')}</span><span><b>${esc(fmtState(l))}</b><small>${esc(nameOf(l))}</small></span></button>`);
  }

  return `
    <section class="hero">
      <h1>${greeting()}</h1>
      <p>${esc(date.charAt(0).toUpperCase() + date.slice(1))}${alerts.length ? ` · <span class="txt-bad">${alerts.length} point${alerts.length > 1 ? 's' : ''} à vérifier</span>` : ' · tout est en ordre'}</p>
    </section>
    <div class="stats">${chips.join('')}</div>
    ${alerts.length ? `<section><h2 class="section-title">À vérifier</h2><div class="alerts">${alerts.map(e => `
      <button class="alert-row" data-act="open" data-id="${esc(e.entity_id)}">${icon(entityIcon(e))}<span><b>${esc(nameOf(e))}</b> · ${esc(fmtState(e))}</span><small>${relTime(e.last_changed)}</small></button>`).join('')}</div></section>` : ''}
    ${scenes.length ? `<section><h2 class="section-title">Scènes <a href="#/scenes">Tout voir</a></h2><div class="scene-strip">${scenes.slice(0, 8).map(sceneButton).join('')}</div></section>` : ''}
    <section><h2 class="section-title">Favoris ${S.favs ? '' : '<small class="muted">(suggestion — étoile ★ dans le détail d’un appareil)</small>'}</h2>
      ${favs.length ? `<div class="tiles">${favs.map(e => tile(e)).join('')}</div>` : `<div class="empty">${icon('star')}<p>Aucun favori. Ouvre un appareil et touche l’étoile pour l’ajouter ici.</p></div>`}
    </section>
    <section><h2 class="section-title">Pièces <a href="#/rooms">Tout voir</a></h2><div class="rooms">${rooms().map(roomCard).join('')}</div></section>`;
}

function viewRooms() {
  const list = rooms();
  return `<h1 class="page-title">Pièces</h1>
    ${list.length ? `<div class="rooms">${list.map(roomCard).join('')}</div>` : `<div class="empty">${icon('rooms')}<p>Aucun appareil trouvé.</p></div>`}`;
}

function roomCard(areaId) {
  const devs = devicesIn(areaId);
  const lights = devs.filter(e => domainOf(e.entity_id) === 'light');
  const on = lights.filter(e => e.state === 'on').length;
  const covers = devs.filter(e => domainOf(e.entity_id) === 'cover');
  const openCovers = covers.filter(e => e.state !== 'closed').length;
  const t = roomTemp(areaId);
  const alerts = devs.filter(isAlert).length;
  const bits = [];
  if (lights.length) bits.push(`<span class="${on ? 'txt-light' : ''}">${icon('bulb')}${on}/${lights.length}</span>`);
  if (covers.length) bits.push(`<span>${icon('blinds')}${openCovers ? openCovers + ' ouvert' + (openCovers > 1 ? 's' : '') : 'fermés'}</span>`);
  if (alerts) bits.push(`<span class="txt-bad">${icon('alert')}${alerts}</span>`);
  return `<a class="room-card${on ? ' lit' : ''}" href="#/room/${encodeURIComponent(areaId)}">
    <span class="room-top"><b>${esc(areaName(areaId))}</b>${t != null ? `<span class="room-temp">${fmtNum(t)}°</span>` : ''}</span>
    <span class="room-meta">${bits.join('') || `<span>${devs.length} appareil${devs.length > 1 ? 's' : ''}</span>`}</span>
  </a>`;
}

function viewRoom(areaId) {
  const devs = devicesIn(areaId);
  const room = areaName(areaId);
  if (!devs.length) return `<a class="back" href="#/rooms">${icon('back')} Pièces</a><div class="empty">${icon('rooms')}<p>Cette pièce n’existe pas ou ne contient aucun appareil.</p></div>`;
  const sensors = devs.filter(e => ['sensor', 'binary_sensor'].includes(domainOf(e.entity_id)));
  const controls = devs.filter(e => !sensors.includes(e));
  const lightsOn = devs.filter(e => domainOf(e.entity_id) === 'light' && e.state === 'on');
  const covers = devs.filter(e => domainOf(e.entity_id) === 'cover');
  return `<a class="back" href="#/rooms">${icon('back')} Pièces</a>
    <div class="room-head">
      <h1 class="page-title">${esc(room)}</h1>
      <div class="room-actions">
        ${covers.length ? `<button class="btn" data-act="area-covers" data-svc="open_cover" data-area="${esc(areaId)}">${icon('up')} Volets</button><button class="btn" data-act="area-covers" data-svc="close_cover" data-area="${esc(areaId)}">${icon('down')} Volets</button>` : ''}
        ${lightsOn.length ? `<button class="btn" data-act="all-off" data-area="${esc(areaId)}">${icon('power')} Tout éteindre</button>` : ''}
      </div>
    </div>
    ${sensors.length ? `<div class="sensor-row">${sensors.map(e => sensorChip(e, room)).join('')}</div>` : ''}
    ${controls.length ? `<div class="tiles">${controls.map(e => tile(e, room)).join('')}</div>` : ''}`;
}

function sensorChip(e, room) {
  return `<button class="sensor-chip${isAlert(e) ? ' alert' : ''}${domainOf(e.entity_id) === 'binary_sensor' && e.state === 'on' ? ' active' : ''}" data-act="open" data-id="${esc(e.entity_id)}">
    ${icon(entityIcon(e))}<span><b>${esc(fmtState(e))}</b><small>${esc(nameOf(e, room))}</small></span></button>`;
}

function tile(e, room) {
  const id = e.entity_id, d = domainOf(id);
  const on = isOn(e), un = isUnavailable(e);
  const name = nameOf(e, room);
  const toggles = TOGGLE_DOMAINS.includes(d) || d === 'lock';
  const classes = ['tile', 'd-' + d];
  if (on && !un) classes.push('on');
  if (un) classes.push('unavailable');
  if (isAlert(e)) classes.push('alert');
  let style = '';
  if (d === 'light' && e.state === 'on') {
    const rgb = e.attributes.color_mode === 'hs' || e.attributes.color_mode === 'rgb' || e.attributes.color_mode === 'xy' ? e.attributes.rgb_color : null;
    if (rgb) style = ` style="--glow: rgb(${rgb.join(',')})"`;
  }
  let controls = '';
  if (d === 'cover' && !un) {
    controls = `<div class="tile-controls">
      <button class="icon-btn" data-act="cover" data-svc="open_cover" data-id="${esc(id)}" aria-label="Ouvrir ${esc(name)}">${icon('up')}</button>
      <button class="icon-btn" data-act="cover" data-svc="stop_cover" data-id="${esc(id)}" aria-label="Arrêter ${esc(name)}">${icon('stop')}</button>
      <button class="icon-btn" data-act="cover" data-svc="close_cover" data-id="${esc(id)}" aria-label="Fermer ${esc(name)}">${icon('down')}</button></div>`;
  } else if (d === 'climate' && !un && e.state !== 'off') {
    const target = S.pendingTemp[id] ?? e.attributes.temperature;
    controls = `<div class="tile-controls">
      <button class="icon-btn" data-act="temp" data-dir="-1" data-id="${esc(id)}" aria-label="Baisser la consigne">${icon('minus')}</button>
      <span class="target">${target != null ? fmtNum(target) + '°' : '—'}</span>
      <button class="icon-btn" data-act="temp" data-dir="1" data-id="${esc(id)}" aria-label="Monter la consigne">${icon('plus')}</button></div>`;
  }
  let stateText = fmtState(e);
  if (d === 'climate' && e.attributes.current_temperature != null) stateText = `${fmtNum(e.attributes.current_temperature)} °C · ${HVAC_LABELS[e.state] || e.state}`;
  return `<div class="${classes.join(' ')}"${style}>
    <button class="tile-main" data-act="${toggles && !un ? 'toggle' : 'open'}" data-id="${esc(id)}" data-press="open">
      <span class="tile-icon">${icon(entityIcon(e))}</span>
      <span class="tile-text"><span class="tile-name">${esc(name)}</span><span class="tile-state">${esc(stateText)}</span></span>
    </button>
    <button class="tile-more" data-act="open" data-id="${esc(id)}" aria-label="Détails : ${esc(name)}">${icon('more')}</button>
    ${controls}
  </div>`;
}

function sceneButton(e) {
  const ic = e.attributes.icon_name || (domainOf(e.entity_id) === 'script' ? 'play' : 'sparkle');
  return `<button class="scene" data-act="scene" data-id="${esc(e.entity_id)}">${icon(ICONS[ic] ? ic : 'sparkle')}<span>${esc(nameOf(e))}</span></button>`;
}

function viewScenes() {
  const scenes = ofDomain('scene').sort((a, b) => nameOf(a).localeCompare(nameOf(b), 'fr'));
  const scripts = ofDomain('script').sort((a, b) => nameOf(a).localeCompare(nameOf(b), 'fr'));
  return `<h1 class="page-title">Scènes</h1>
    <p class="muted">Une scène règle plusieurs appareils d’un coup.</p>
    ${scenes.length ? `<div class="scene-grid">${scenes.map(sceneButton).join('')}</div>` : `<div class="empty">${icon('sparkle')}<p>Aucune scène. Crée-en dans Home Assistant : Paramètres → Automatisations et scènes → Scènes.</p></div>`}
    ${scripts.length ? `<h2 class="section-title">Scripts</h2><div class="scene-grid">${scripts.map(sceneButton).join('')}</div>` : ''}`;
}

function viewAutomations() {
  const list = ofDomain('automation').sort((a, b) => nameOf(a).localeCompare(nameOf(b), 'fr'));
  return `<h1 class="page-title">Automatismes</h1>
    <p class="muted">Active, désactive ou lance à la main les automatisations de la maison.</p>
    ${list.length ? `<div class="list">${list.map(e => {
      const on = e.state === 'on';
      return `<div class="list-row${on ? '' : ' dim'}">
        <span class="list-ic">${icon('robot')}</span>
        <span class="list-text"><b>${esc(nameOf(e))}</b>
          ${e.attributes.description ? `<small>${esc(e.attributes.description)}</small>` : ''}
          <small class="muted">Dernier déclenchement : ${relTime(e.attributes.last_triggered)}</small></span>
        <button class="btn small" data-act="auto-run" data-id="${esc(e.entity_id)}" aria-label="Exécuter maintenant">${icon('play')}<span class="hide-sm">Lancer</span></button>
        <button class="switch${on ? ' on' : ''}" role="switch" aria-checked="${on}" data-act="toggle" data-id="${esc(e.entity_id)}" aria-label="${on ? 'Désactiver' : 'Activer'} ${esc(nameOf(e))}"><span></span></button>
      </div>`;
    }).join('')}</div>` : `<div class="empty">${icon('robot')}<p>Aucune automatisation.</p></div>`}`;
}

function viewLog() {
  return `<div class="room-head"><h1 class="page-title">Journal</h1>${S.log.length ? '<button class="btn" data-act="clear-log">Effacer</button>' : ''}</div>
    <p class="muted">Les derniers changements d’état vus par l’application (capteurs numériques exclus).</p>
    ${S.log.length ? `<div class="list log">${S.log.map(l => `
      <div class="list-row${l.alert ? ' alert' : ''}"><span class="list-ic">${icon(l.icon)}</span>
        <span class="list-text"><b>${esc(l.name)}</b><small>${esc(l.from)} → <b>${esc(l.to)}</b></small></span>
        <time class="muted">${timeHM(l.t)}</time></div>`).join('')}</div>` : `<div class="empty">${icon('list')}<p>Rien pour l’instant. Allume une lumière pour voir apparaître l’historique.</p></div>`}`;
}

function viewSettings() {
  const s = S.settings;
  const mixed = location.protocol === 'https:' && /^http:/i.test(s.url);
  return `<h1 class="page-title">Réglages</h1>
  <form class="card form${s.mode === 'demo' ? ' demo' : ''}" id="settingsForm" autocomplete="off">
    <h2>Connexion</h2>
    <div class="segmented" role="radiogroup" aria-label="Source des données">
      <label><input type="radio" name="mode" value="demo" ${s.mode === 'demo' ? 'checked' : ''}><span>Maison de démo</span></label>
      <label><input type="radio" name="mode" value="ha" ${s.mode === 'ha' ? 'checked' : ''}><span>Home Assistant</span></label>
    </div>
    <div class="ha-fields">
      <label class="field"><span>Adresse de Home Assistant</span>
        <input name="url" type="url" inputmode="url" placeholder="http://homeassistant.local:8123" value="${esc(s.url)}"></label>
      <label class="field"><span>Jeton d’accès longue durée</span>
        <input name="token" type="password" placeholder="eyJhbGciOi…" value="${esc(s.token)}"></label>
      <p class="help">Dans Home Assistant : clique sur ton profil (en bas à gauche) → onglet <b>Sécurité</b> → <b>Jetons d’accès longue durée</b> → Créer un jeton. Le jeton reste uniquement dans ce navigateur.</p>
      ${mixed ? `<p class="help warn">${icon('alert')} Cette page est en HTTPS mais l’adresse de Home Assistant est en HTTP : le navigateur bloquera la connexion. Utilise une adresse HTTPS (Nabu Casa, DuckDNS…) ou installe l’application directement dans Home Assistant (voir le README).</p>` : ''}
    </div>
    <div class="form-actions"><button class="btn primary" type="submit">Enregistrer et se connecter</button></div>
  </form>

  <div class="card form">
    <h2>Apparence</h2>
    <div class="segmented" role="radiogroup" aria-label="Thème">
      ${[['auto', 'Automatique'], ['light', 'Clair'], ['dark', 'Sombre']].map(([v, l]) => `<label><input type="radio" name="theme" value="${v}" data-act="theme" ${s.theme === v ? 'checked' : ''}><span>${l}</span></label>`).join('')}
    </div>
    <div class="form-actions">${S.favs ? '<button class="btn" data-act="reset-favs">Revenir aux favoris suggérés</button>' : ''}</div>
  </div>

  <form class="card form" id="energyForm" autocomplete="off">
    <h2>Énergie</h2>
    <label class="field"><span>Prix du kWh (€)</span>
      <input name="price" type="number" inputmode="decimal" min="0" step="0.0001" value="${esc(s.price)}"></label>
    <label class="field"><span>Compteur principal (kWh)</span>
      <select name="energyMeter"><option value="">Automatique (tableau Énergie de Home Assistant)</option>
        ${energySensors().map(e => `<option value="${esc(e.entity_id)}" ${s.energyMeter === e.entity_id ? 'selected' : ''}>${esc(nameOf(e))} — ${esc(e.entity_id)}</option>`).join('')}</select></label>
    <label class="field"><span>Capteur de puissance (W)</span>
      <select name="powerSensor"><option value="">Automatique</option>
        ${powerSensors().map(e => `<option value="${esc(e.entity_id)}" ${s.powerSensor === e.entity_id ? 'selected' : ''}>${esc(nameOf(e))} — ${esc(e.entity_id)}</option>`).join('')}</select></label>
    <p class="help">Le prix sert au coût estimé. Tarif réglementé « Base » : environ 0,25 €/kWh.</p>
    <div class="form-actions"><button class="btn primary" type="submit">Enregistrer</button></div>
  </form>

  <div class="card form">
    <h2>État</h2>
    <p>Source : <b>${s.mode === 'ha' ? 'Home Assistant' : 'Maison de démo'}</b> · ${esc(S.status.text)}</p>
    <p class="muted">${S.ready ? `${devices().length} appareils, ${ofDomain('scene').length} scènes, ${ofDomain('automation').length} automatisations, ${S.areas.length} pièces.` : 'Pas encore de données.'}</p>
    <div class="form-actions"><a class="btn" href="#/log">${icon('list')} Journal des changements</a>${s.mode === 'demo' ? '<button class="btn" data-act="reset-demo">Réinitialiser la maison de démo</button>' : ''}</div>
  </div>`;
}

/* ---------- Fenêtre de détail ---------- */

function openModal(id) {
  S.modalId = id;
  const m = document.getElementById('modal');
  m.hidden = false;
  document.body.classList.add('modal-open');
  renderModal(true);
  m.querySelector('[data-act="close-modal"].icon-btn')?.focus();
}

function closeModal() {
  S.modalId = null;
  S.dragging = false;
  document.getElementById('modal').hidden = true;
  document.body.classList.remove('modal-open');
  document.getElementById('modalCard').innerHTML = '';
}

function renderModal(force) {
  if (!S.modalId) return;
  if (S.dragging && !force) return;
  const e = S.entities[S.modalId];
  const card = document.getElementById('modalCard');
  if (!e) { closeModal(); return; }
  const html = modalHtml(e);
  if (force || card.dataset.html !== html) {
    const focusKey = document.activeElement?.dataset?.key;
    card.innerHTML = html;
    card.dataset.html = html;
    if (focusKey) card.querySelector(`[data-key="${focusKey}"]`)?.focus();
  }
}

function modalHtml(e) {
  const id = e.entity_id, d = domainOf(id), a = e.attributes, un = isUnavailable(e);
  const area = S.areaOf[id];
  const fav = favourites().some(f => f.entity_id === id) && S.favs;
  const canFav = DEVICE_DOMAINS.includes(d);
  let body = '';

  if (!un && ['light', 'switch', 'input_boolean', 'fan'].includes(d)) {
    body += `<button class="power-btn${e.state === 'on' ? ' on' : ''}" data-act="toggle" data-id="${esc(id)}" data-key="power">${icon('power')}<span>${e.state === 'on' ? 'Éteindre' : 'Allumer'}</span></button>`;
  }
  if (!un && d === 'light') {
    if (supportsBrightness(e) && !(a.supported_color_modes || []).every(m => m === 'onoff')) {
      const b = brightnessPct(e) ?? 0;
      body += `<label class="field"><span>Luminosité <output id="bOut">${b} %</output></span>
        <input type="range" min="1" max="100" value="${b || 1}" data-input="brightness" data-id="${esc(id)}" data-key="bright"></label>`;
    }
    if (supportsColorTemp(e)) {
      body += `<div class="field"><span>Blanc</span><div class="chips">${[[2700, 'Chaud'], [4000, 'Neutre'], [6000, 'Froid']].map(([k, l]) =>
        `<button class="chip ct${a.color_mode === 'color_temp' && Math.abs((a.color_temp_kelvin || 0) - k) < 400 && e.state === 'on' ? ' sel' : ''}" style="--c:${kelvinCss(k)}" data-act="light-ct" data-k="${k}" data-id="${esc(id)}">${l}</button>`).join('')}</div></div>`;
    }
    if (supportsColor(e)) {
      body += `<div class="field"><span>Couleur</span><div class="swatches">${[0, 30, 55, 120, 180, 230, 280, 320].map(h =>
        `<button class="swatch" style="--c:hsl(${h} 90% 55%)" data-act="light-hs" data-h="${h}" data-id="${esc(id)}" aria-label="Teinte ${h}°"></button>`).join('')}</div></div>`;
    }
  }
  if (!un && d === 'cover') {
    const p = a.current_position;
    body += `<div class="big-value">${esc(fmtState(e))}</div>
      <div class="btn-row">
        <button class="btn" data-act="cover" data-svc="open_cover" data-id="${esc(id)}">${icon('up')} Ouvrir</button>
        <button class="btn" data-act="cover" data-svc="stop_cover" data-id="${esc(id)}">${icon('stop')} Stop</button>
        <button class="btn" data-act="cover" data-svc="close_cover" data-id="${esc(id)}">${icon('down')} Fermer</button>
      </div>`;
    if (p != null) {
      body += `<label class="field"><span>Position <output id="pOut">${p} %</output></span>
        <input type="range" min="0" max="100" step="5" value="${p}" data-input="position" data-id="${esc(id)}" data-key="pos"></label>
        <div class="chips">${[25, 50, 75].map(v => `<button class="chip" data-act="cover-pos" data-pos="${v}" data-id="${esc(id)}">${v} %</button>`).join('')}</div>`;
    }
  }
  if (!un && d === 'climate') {
    const target = S.pendingTemp[id] ?? a.temperature;
    body += `<div class="thermo">
        <button class="round-btn" data-act="temp" data-dir="-1" data-id="${esc(id)}" data-key="tminus" aria-label="Baisser" ${e.state === 'off' ? 'disabled' : ''}>${icon('minus')}</button>
        <div class="thermo-val"><b>${target != null && e.state !== 'off' ? fmtNum(target) + '°' : 'Arrêt'}</b><small>Actuellement ${a.current_temperature != null ? fmtNum(a.current_temperature) + ' °C' : '—'}</small></div>
        <button class="round-btn" data-act="temp" data-dir="1" data-id="${esc(id)}" data-key="tplus" aria-label="Monter" ${e.state === 'off' ? 'disabled' : ''}>${icon('plus')}</button>
      </div>
      <div class="field"><span>Mode</span><div class="chips">${(a.hvac_modes || ['off', 'heat']).map(m =>
        `<button class="chip${e.state === m ? ' sel' : ''}" data-act="hvac" data-mode="${esc(m)}" data-id="${esc(id)}">${esc(HVAC_LABELS[m] || m)}</button>`).join('')}</div></div>`;
  }
  if (!un && d === 'lock') {
    const locked = e.state === 'locked';
    body += `<button class="power-btn ${locked ? 'locked' : 'on'}" data-act="toggle" data-id="${esc(id)}" data-key="lock">${icon(locked ? 'lock' : 'unlock')}<span>${locked ? 'Déverrouiller' : 'Verrouiller'}</span></button>`;
  }
  if (un || ['sensor', 'binary_sensor'].includes(d)) {
    body += `<div class="big-value${isAlert(e) ? ' txt-bad' : ''}">${esc(fmtState(e))}</div>`;
  }

  return `<div class="modal-head">
      <span class="tile-icon${isOn(e) && !un ? ' on' : ''} d-${d}">${icon(entityIcon(e))}</span>
      <div class="modal-title"><h2 id="modalTitle">${esc(nameOf(e))}</h2><small>${area ? esc(areaName(area)) + ' · ' : ''}${esc(fmtState(e)) || esc(id)}</small></div>
      ${canFav ? `<button class="icon-btn fav${fav ? ' sel' : ''}" data-act="fav" data-id="${esc(id)}" aria-pressed="${!!fav}" aria-label="${fav ? 'Retirer des' : 'Ajouter aux'} favoris">${icon('star')}</button>` : ''}
      <button class="icon-btn" data-act="close-modal" aria-label="Fermer">${icon('close')}</button>
    </div>
    <div class="modal-body">${body}</div>
    <div class="modal-foot muted">Modifié ${relTime(e.last_changed)} · <code>${esc(id)}</code></div>`;
}

function kelvinCss(k) {
  return k <= 3000 ? '#ffb46b' : k <= 4500 ? '#ffe4c4' : '#dbe9ff';
}

/* ---------- Actions ---------- */

const tempTimers = {};
function bumpTemp(id, dir) {
  const e = S.entities[id];
  if (!e) return;
  const a = e.attributes, step = a.target_temp_step || 0.5;
  const cur = S.pendingTemp[id] ?? a.temperature ?? a.current_temperature ?? 20;
  const next = Math.min(a.max_temp ?? 35, Math.max(a.min_temp ?? 5, Math.round((cur + dir * step) / step) * step));
  S.pendingTemp[id] = next;
  scheduleRender();
  clearTimeout(tempTimers[id]);
  tempTimers[id] = setTimeout(async () => {
    await act('climate', 'set_temperature', { temperature: next }, id);
    setTimeout(() => { if (S.pendingTemp[id] === next) { delete S.pendingTemp[id]; scheduleRender(); } }, 3000);
  }, 600);
}

function toggleEntity(id) {
  const e = S.entities[id];
  if (!e) return;
  const d = domainOf(id);
  if (d === 'lock') return act('lock', e.state === 'locked' ? 'unlock' : 'lock', {}, id);
  if (d === 'automation') return act('automation', e.state === 'on' ? 'turn_off' : 'turn_on', {}, id);
  return act(d, 'toggle', {}, id);
}

const ACTIONS = {
  toggle: el => toggleEntity(el.dataset.id),
  open: el => openModal(el.dataset.id),
  'close-modal': () => closeModal(),
  cover: el => act('cover', el.dataset.svc, {}, el.dataset.id),
  'cover-pos': el => act('cover', 'set_cover_position', { position: Number(el.dataset.pos) }, el.dataset.id),
  temp: el => bumpTemp(el.dataset.id, Number(el.dataset.dir)),
  hvac: el => act('climate', 'set_hvac_mode', { hvac_mode: el.dataset.mode }, el.dataset.id),
  'light-ct': el => act('light', 'turn_on', { color_temp_kelvin: Number(el.dataset.k) }, el.dataset.id),
  'light-hs': el => act('light', 'turn_on', { hs_color: [Number(el.dataset.h), 85] }, el.dataset.id),
  scene: el => {
    const e = S.entities[el.dataset.id];
    act(domainOf(el.dataset.id), 'turn_on', {}, el.dataset.id, `« ${nameOf(e)} » lancée`);
  },
  'auto-run': el => act('automation', 'trigger', {}, el.dataset.id, `« ${nameOf(S.entities[el.dataset.id])} » exécutée`),
  'all-off': el => {
    const area = el.dataset.area;
    const ids = (area ? devicesIn(area) : devices()).filter(e => domainOf(e.entity_id) === 'light' && e.state === 'on').map(e => e.entity_id);
    if (ids.length) act('light', 'turn_off', {}, ids, `${ids.length} lumière${ids.length > 1 ? 's' : ''} éteinte${ids.length > 1 ? 's' : ''}`);
  },
  'area-covers': el => {
    const ids = devicesIn(el.dataset.area).filter(e => domainOf(e.entity_id) === 'cover').map(e => e.entity_id);
    if (ids.length) act('cover', el.dataset.svc, {}, ids);
  },
  fav: el => {
    const id = el.dataset.id;
    const current = favourites().map(e => e.entity_id);
    const list = S.favs ? [...S.favs] : current;
    const i = list.indexOf(id);
    if (S.favs && i >= 0) list.splice(i, 1);
    else if (i < 0) list.push(id);
    S.favs = list;
    store.set('domo.favs', S.favs);
    toast(S.favs.includes(id) ? 'Ajouté aux favoris' : 'Retiré des favoris', 'ok');
    scheduleRender();
  },
  'reset-favs': () => { S.favs = null; localStorage.removeItem('domo.favs'); toast('Favoris réinitialisés', 'ok'); render(); },
  'clear-log': () => { S.log = []; store.set('domo.log', []); render(); },
  'reset-demo': () => {
    if (!confirm('Remettre la maison de démo dans son état d’origine ?')) return;
    backend?.reset?.();
    startBackend();
    toast('Maison de démo réinitialisée', 'ok');
  },
  'energy-period': el => {
    S.energy.period = el.dataset.period;
    store.set('domo.energyPeriod', S.energy.period);
    render();
  },
  'energy-reload': () => { loadEnergy(true); toast('Actualisation…', 'ok'); },
  'use-demo': () => { saveSettings({ mode: 'demo' }); location.hash = '#/home'; },
  theme: el => { saveSettings({ theme: el.value }, false); },
};

function saveSettings(patch, reconnect = true) {
  S.settings = { ...S.settings, ...patch };
  store.set('domo.settings', S.settings);
  applyTheme();
  if (reconnect) startBackend();
  render();
}

function applyTheme() {
  const t = S.settings.theme;
  if (t === 'auto') delete document.documentElement.dataset.theme;
  else document.documentElement.dataset.theme = t;
  const dark = t === 'dark' || (t === 'auto' && matchMedia('(prefers-color-scheme: dark)').matches);
  document.querySelector('meta[name="theme-color"]').content = dark ? '#11151c' : '#f4f6f9';
}

/* ---------- Événements ---------- */

let pressTimer = null, pressFired = false, pressStart = null;

document.addEventListener('click', ev => {
  if (pressFired) { pressFired = false; ev.preventDefault(); return; }
  const el = ev.target.closest('[data-act]');
  if (!el || el.tagName === 'INPUT') return;
  const fn = ACTIONS[el.dataset.act];
  if (fn) { ev.preventDefault(); fn(el); }
});

document.addEventListener('change', ev => {
  const el = ev.target;
  if (el.dataset.act === 'theme') return ACTIONS.theme(el);
  if (el.dataset.input === 'brightness') act('light', 'turn_on', { brightness_pct: Number(el.value) }, el.dataset.id);
  if (el.dataset.input === 'position') act('cover', 'set_cover_position', { position: Number(el.value) }, el.dataset.id);
  if (el.dataset.input) { S.dragging = false; }
  if (el.name === 'mode' && el.form?.id === 'settingsForm') el.form.classList.toggle('demo', el.value === 'demo');
});

document.addEventListener('input', ev => {
  const el = ev.target;
  if (el.dataset.input === 'brightness') document.getElementById('bOut').textContent = el.value + ' %';
  if (el.dataset.input === 'position') document.getElementById('pOut').textContent = el.value + ' %';
});

document.addEventListener('pointerdown', ev => {
  if (ev.target.matches('input[type=range]')) { S.dragging = true; return; }
  const el = ev.target.closest('[data-press]');
  if (!el) return;
  pressFired = false;
  pressStart = [ev.clientX, ev.clientY];
  clearTimeout(pressTimer);
  pressTimer = setTimeout(() => { pressFired = true; openModal(el.dataset.id); }, 550);
});
document.addEventListener('pointermove', ev => {
  if (pressStart && Math.hypot(ev.clientX - pressStart[0], ev.clientY - pressStart[1]) > 10) { clearTimeout(pressTimer); pressStart = null; }
});
['pointerup', 'pointercancel'].forEach(t => document.addEventListener(t, ev => {
  clearTimeout(pressTimer);
  pressStart = null;
  if (ev.target.matches?.('input[type=range]')) setTimeout(() => { S.dragging = false; }, 300);
}, true));
document.addEventListener('contextmenu', ev => { if (ev.target.closest('[data-press]')) ev.preventDefault(); });

document.addEventListener('submit', ev => {
  if (ev.target.id === 'energyForm') {
    ev.preventDefault();
    const f = new FormData(ev.target);
    const price = Number(String(f.get('price')).replace(',', '.'));
    document.activeElement?.blur();
    saveSettings({ price: Number.isFinite(price) && price >= 0 ? price : 0, energyMeter: f.get('energyMeter') || '', powerSensor: f.get('powerSensor') || '' }, false);
    Object.assign(S.energy, { key: null, data: null });
    toast('Réglages énergie enregistrés', 'ok');
    return;
  }
  if (ev.target.id !== 'settingsForm') return;
  ev.preventDefault();
  const f = new FormData(ev.target);
  const mode = f.get('mode');
  const url = String(f.get('url') || '').trim();
  const token = String(f.get('token') || '').trim();
  if (mode === 'ha' && (!url || !token)) { toast('Indique l’adresse et le jeton de Home Assistant.', 'error'); return; }
  if (mode === 'ha' && !/^https?:\/\//i.test(url)) { toast('L’adresse doit commencer par http:// ou https://', 'error'); return; }
  document.activeElement?.blur();
  saveSettings({ mode, url, token });
  toast(mode === 'ha' ? 'Connexion à Home Assistant…' : 'Maison de démo chargée', 'ok');
  location.hash = '#/home';
});

document.addEventListener('keydown', ev => {
  if (ev.key === 'Escape' && S.modalId) closeModal();
});

window.addEventListener('hashchange', () => { closeModal(); window.scrollTo(0, 0); render(); document.getElementById('view').focus({ preventScroll: true }); });
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyTheme);
let resizeTimer;
window.addEventListener('resize', () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(scheduleRender, 150); });

/* ---------- Divers ---------- */

function toast(msg, kind = '') {
  const box = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = 'toast ' + kind;
  el.innerHTML = `${icon(kind === 'error' ? 'alert' : 'check')}<span>${esc(msg)}</span>`;
  box.appendChild(el);
  while (box.children.length > 3) box.firstChild.remove();
  setTimeout(() => { el.classList.add('out'); setTimeout(() => el.remove(), 300); }, kind === 'error' ? 6000 : 2500);
}

function tickClock() {
  document.getElementById('clock').textContent = new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function init() {
  document.getElementById('brandLogo').innerHTML = icon('home');
  applyTheme();
  tickClock();
  setInterval(() => { tickClock(); scheduleRender(); }, 30000);
  startBackend();
  render();
  if ('serviceWorker' in navigator && location.protocol !== 'file:') {
    navigator.serviceWorker.register('sw.js').catch(() => { /* hors ligne non disponible */ });
  }
}

init();
