/* Fonctions d'affichage communes : noms, états, icônes, nombres. */

const SUPPORTED_DOMAINS = ['light', 'switch', 'input_boolean', 'fan', 'cover', 'climate', 'lock', 'sensor', 'binary_sensor', 'scene', 'script', 'automation'];
const TOGGLE_DOMAINS = ['light', 'switch', 'input_boolean', 'fan', 'automation'];
const DEVICE_DOMAINS = ['light', 'switch', 'input_boolean', 'fan', 'cover', 'climate', 'lock', 'sensor', 'binary_sensor'];
const SENSOR_CLASSES = ['temperature', 'humidity', 'power', 'energy', 'illuminance', 'carbon_dioxide', 'pm25', 'battery', 'pressure'];
const OPENING_CLASSES = ['door', 'window', 'opening', 'garage_door'];
const PRESENCE_CLASSES = ['motion', 'occupancy', 'presence'];
const DANGER_CLASSES = ['moisture', 'smoke', 'gas', 'carbon_monoxide', 'safety', 'problem'];

const HVAC_LABELS = { off: 'Arrêt', heat: 'Chauffage', cool: 'Clim', auto: 'Auto', heat_cool: 'Auto', dry: 'Déshum.', fan_only: 'Ventilation' };

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function domainOf(id) { return id.split('.')[0]; }

function nameOf(e, roomName) {
  let n = e.attributes?.friendly_name || e.entity_id.split('.')[1].replace(/_/g, ' ');
  if (roomName) {
    const re = new RegExp('^' + roomName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\s+', 'i');
    const short = n.replace(re, '');
    if (short) n = short.charAt(0).toUpperCase() + short.slice(1);
  }
  return n;
}

const nf1 = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });
const nf0 = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
function fmtNum(v, digits = 1) {
  const n = Number(v);
  if (!Number.isFinite(n)) return String(v);
  return (digits === 0 ? nf0 : nf1).format(n);
}

function isUnavailable(e) { return !e || e.state === 'unavailable' || e.state === 'unknown'; }

function isOn(e) {
  if (!e) return false;
  const d = domainOf(e.entity_id);
  if (d === 'cover') return e.state !== 'closed';
  if (d === 'lock') return e.state !== 'locked';
  if (d === 'climate') return e.state !== 'off';
  return e.state === 'on';
}

function brightnessPct(e) {
  const b = e.attributes?.brightness;
  return b == null ? null : Math.round(b / 2.55);
}

function supportsBrightness(e) {
  const modes = e.attributes?.supported_color_modes || [];
  return modes.some(m => m !== 'onoff') || e.attributes?.brightness != null;
}
function supportsColor(e) {
  const modes = e.attributes?.supported_color_modes || [];
  return modes.some(m => ['hs', 'rgb', 'rgbw', 'rgbww', 'xy'].includes(m));
}
function supportsColorTemp(e) {
  return (e.attributes?.supported_color_modes || []).includes('color_temp');
}

function fmtState(e) {
  if (!e) return '—';
  if (e.state === 'unavailable') return 'Indisponible';
  if (e.state === 'unknown') return 'Inconnu';
  const a = e.attributes || {};
  switch (domainOf(e.entity_id)) {
    case 'light': {
      if (e.state !== 'on') return 'Éteinte';
      const b = brightnessPct(e);
      return b != null ? `Allumée · ${b} %` : 'Allumée';
    }
    case 'switch': case 'input_boolean': case 'fan':
      return e.state === 'on' ? 'Allumé' : 'Éteint';
    case 'automation':
      return e.state === 'on' ? 'Active' : 'Désactivée';
    case 'cover': {
      const p = a.current_position;
      if (e.state === 'opening') return `Ouverture…${p != null ? ' ' + p + ' %' : ''}`;
      if (e.state === 'closing') return `Fermeture…${p != null ? ' ' + p + ' %' : ''}`;
      if (e.state === 'closed') return 'Fermé';
      if (p != null && p < 100) return `Ouvert · ${p} %`;
      return 'Ouvert';
    }
    case 'climate': {
      const mode = HVAC_LABELS[e.state] || e.state;
      if (e.state === 'off') return mode;
      return a.temperature != null ? `${mode} · ${fmtNum(a.temperature)} °C` : mode;
    }
    case 'lock':
      return { locked: 'Verrouillée', unlocked: 'Déverrouillée', locking: 'Verrouillage…', unlocking: 'Déverrouillage…', jammed: 'Bloquée !', open: 'Ouverte' }[e.state] || e.state;
    case 'binary_sensor': {
      const c = a.device_class, on = e.state === 'on';
      if (OPENING_CLASSES.includes(c)) return on ? 'Ouvert' : 'Fermé';
      if (PRESENCE_CLASSES.includes(c)) return on ? 'Présence' : 'Calme';
      if (c === 'moisture') return on ? 'Fuite !' : 'Sec';
      if (c === 'smoke') return on ? 'Fumée !' : 'RAS';
      if (DANGER_CLASSES.includes(c)) return on ? 'Alerte !' : 'RAS';
      return on ? 'Actif' : 'Inactif';
    }
    case 'sensor': {
      const u = a.unit_of_measurement ? ' ' + a.unit_of_measurement : '';
      return isNaN(Number(e.state)) ? e.state : fmtNum(e.state, a.device_class === 'power' ? 0 : 1) + u;
    }
    case 'scene': case 'script':
      return '';
    default:
      return e.state;
  }
}

function entityIcon(e) {
  const d = domainOf(e.entity_id), a = e.attributes || {};
  const id = e.entity_id;
  switch (d) {
    case 'light': return 'bulb';
    case 'fan': return 'fan';
    case 'switch': case 'input_boolean':
      if (/tv|tele/.test(id)) return 'tv';
      if (/cafe|coffee/.test(id)) return 'coffee';
      if (/arros|water|pompe/.test(id)) return 'water';
      return 'plug';
    case 'cover': return 'blinds';
    case 'climate': return e.state === 'cool' ? 'snow' : 'flame';
    case 'lock': return e.state === 'locked' ? 'lock' : 'unlock';
    case 'binary_sensor':
      if (a.device_class === 'window') return 'window';
      if (OPENING_CLASSES.includes(a.device_class)) return 'door';
      if (PRESENCE_CLASSES.includes(a.device_class)) return 'motion';
      if (a.device_class === 'moisture') return 'drop';
      if (DANGER_CLASSES.includes(a.device_class)) return 'alert';
      return 'sensor';
    case 'sensor':
      return { temperature: 'thermo', humidity: 'drop', power: 'bolt', energy: 'bolt', battery: 'battery', illuminance: 'sun', carbon_dioxide: 'leaf', pm25: 'leaf', pressure: 'gauge' }[a.device_class] || 'sensor';
    case 'scene': return 'sparkle';
    case 'script': return 'play';
    case 'automation': return 'robot';
    default: return 'dot';
  }
}

/* Une entité « alerte » : ouverture ouverte, fuite, fumée, serrure déverrouillée. */
function isAlert(e) {
  if (isUnavailable(e)) return false;
  const d = domainOf(e.entity_id), c = e.attributes?.device_class;
  if (d === 'binary_sensor' && e.state === 'on' && (OPENING_CLASSES.includes(c) || DANGER_CLASSES.includes(c))) return true;
  if (d === 'lock' && (e.state === 'unlocked' || e.state === 'jammed' || e.state === 'open')) return true;
  return false;
}

/* Filtre les entités à afficher (évite le bruit des capteurs techniques). */
function isVisibleDevice(e, reg) {
  const d = domainOf(e.entity_id);
  if (!DEVICE_DOMAINS.includes(d)) return false;
  if (reg && (reg.hidden_by || reg.entity_category)) return false;
  if (e.attributes?.hidden) return false;
  if (d === 'sensor') return SENSOR_CLASSES.includes(e.attributes?.device_class);
  return true;
}

function relTime(iso) {
  if (!iso) return 'jamais';
  const t = new Date(iso).getTime();
  if (!t) return 'jamais';
  const s = Math.round((Date.now() - t) / 1000);
  if (s < 45) return 'à l’instant';
  if (s < 3600) return `il y a ${Math.round(s / 60)} min`;
  if (s < 86400) return `il y a ${Math.round(s / 3600)} h`;
  return new Date(t).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

function timeHM(ts) {
  return new Date(ts).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}
