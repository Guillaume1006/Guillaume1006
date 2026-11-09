/* Page Énergie : consommation (kWh) par heure ou par jour, courbe de puissance sur 24 h,
   répartition par appareil, coût estimé. Données : statistiques long terme de Home Assistant
   (tableau de bord Énergie) ou la maison de démo. Graphiques SVG faits main, sans bibliothèque. */

const PERIODS = {
  day: { label: 'Aujourd’hui', stat: 'hour', days: 1, prev: 'vs hier à la même heure', chart: 'Consommation par heure' },
  week: { label: '7 jours', stat: 'day', days: 7, prev: 'vs les 7 jours d’avant', chart: 'Consommation par jour' },
  month: { label: '30 jours', stat: 'day', days: 30, prev: 'vs les 30 jours d’avant', chart: 'Consommation par jour' },
};
const DAY = 86400e3;
const LINE_CHARTS = {}; // données des courbes pour le survol

/* ---------- Sources ---------- */

function energySensors() {
  return all().filter(e => domainOf(e.entity_id) === 'sensor' && e.attributes.device_class === 'energy');
}
function powerSensors() {
  return all().filter(e => domainOf(e.entity_id) === 'sensor' && e.attributes.device_class === 'power');
}

function energySources(prefs) {
  let meters = [], devs = [];
  for (const src of prefs?.energy_sources || []) {
    if (src.type === 'grid') for (const f of src.flow_from || []) if (f.stat_energy_from) meters.push(f.stat_energy_from);
  }
  for (const d of prefs?.device_consumption || []) {
    if (d.stat_consumption) devs.push({ id: d.stat_consumption, name: d.name || statName(d.stat_consumption) });
  }
  const chosen = S.settings.energyMeter;
  if (chosen) meters = [chosen];
  if (!meters.length) {
    // pas de tableau Énergie configuré : on devine le compteur principal
    const list = energySensors().filter(e => ['total_increasing', 'total'].includes(e.attributes.state_class) || e.attributes.unit_of_measurement?.endsWith('Wh'));
    const main = list.find(e => /linky|compteur|total|maison|home|grid|reseau|réseau|consommation|import/i.test(e.entity_id + ' ' + nameOf(e)))
      || [...list].sort((a, b) => Number(b.state) - Number(a.state))[0];
    if (main) meters = [main.entity_id];
    if (!devs.length) devs = list.filter(e => e !== main).slice(0, 8).map(e => ({ id: e.entity_id, name: nameOf(e) }));
  }
  devs = devs.filter(d => !meters.includes(d.id));
  const power = S.settings.powerSensor || mainPowerEntity()?.entity_id || null;
  return { meters, devs, power };
}

function statName(id) {
  const e = S.entities[id];
  return e ? nameOf(e) : id.replace(/^sensor\./, '').replace(/_/g, ' ');
}

function mainPowerEntity() {
  const p = powerSensors().filter(e => !isUnavailable(e));
  if (!p.length) return null;
  return p.find(e => /maison|total|home|consommation|compteur|linky/i.test(e.entity_id + ' ' + nameOf(e)))
    || [...p].sort((a, b) => Number(b.state) - Number(a.state))[0];
}

function toWatts(v, unit) { return unit === 'kW' ? v * 1000 : v; }

/* ---------- Chargement ---------- */

function loadEnergy(force) {
  const E = S.energy;
  const key = (backend?.kind || '') + ':' + E.period + ':' + (S.settings.energyMeter || '') + ':' + (S.settings.powerSensor || '');
  if (!backend || !S.ready) return;
  if (!force && E.loadingKey === key) return;
  if (!force && E.key === key && Date.now() - E.at < (E.error ? 30e3 : 5 * 60e3)) return;
  const token = ++E.token;
  E.loadingKey = key;
  fetchEnergy(E.period)
    .then(data => { if (token === E.token) Object.assign(E, { data, error: null }); })
    .catch(err => { if (token === E.token) Object.assign(E, { error: err.message || String(err) }); })
    .finally(() => {
      if (token !== E.token) return;
      Object.assign(E, { key, at: Date.now(), loadingKey: null });
      scheduleRender();
    });
}

async function fetchEnergy(period) {
  const cfg = PERIODS[period];
  const now = Date.now();
  const start = new Date(); start.setHours(0, 0, 0, 0); start.setDate(start.getDate() - (cfg.days - 1));
  const prevStart = new Date(start); prevStart.setDate(prevStart.getDate() - cfg.days);
  const elapsed = now - start.getTime();

  const prefs = backend.energyPrefs ? await backend.energyPrefs() : null;
  const src = energySources(prefs);
  if (!src.meters.length) return { period, empty: true, power: await fetchPower(src.power, now) };

  const ids = [...new Set([...src.meters, ...src.devs.map(d => d.id)])];
  const [stats, power] = await Promise.all([
    backend.statistics(ids, prevStart, new Date(now), cfg.stat),
    fetchPower(src.power, now),
  ]);

  // créneaux : 24 heures du jour, ou les N derniers jours
  const slots = [];
  if (cfg.stat === 'hour') {
    for (let h = 0; h < 24; h++) { const d = new Date(start); d.setHours(h); slots.push({ t: d.getTime(), kwh: 0, has: false }); }
  } else {
    for (let i = 0; i < cfg.days; i++) { const d = new Date(start); d.setDate(d.getDate() + i); slots.push({ t: d.getTime(), kwh: 0, has: false }); }
  }
  const slotKey = t => cfg.stat === 'hour' ? Math.floor(t / 3600e3) : new Date(t).toDateString();
  const index = new Map(slots.map((s, i) => [slotKey(s.t), i]));
  const slotLen = cfg.stat === 'hour' ? 3600e3 : DAY;
  slots.forEach(s => { s.future = s.t > now; s.partial = s.t <= now && now < s.t + slotLen; });

  let prevTotal = 0, hasPrev = false;
  for (const id of src.meters) {
    for (const row of changes(stats[id])) {
      if (row.t >= start.getTime()) {
        const i = index.get(slotKey(row.t));
        if (i != null) { slots[i].kwh += row.change; slots[i].has = true; }
      } else if (row.t - prevStart.getTime() < elapsed) {
        prevTotal += row.change; hasPrev = true;
      }
    }
  }
  const total = slots.reduce((sum, s) => sum + s.kwh, 0);
  const devs = src.devs.map(d => ({
    name: d.name,
    kwh: changes(stats[d.id]).filter(r => r.t >= start.getTime()).reduce((sum, r) => sum + r.change, 0),
  })).filter(d => d.kwh > 0.005).sort((a, b) => b.kwh - a.kwh);
  const tracked = devs.reduce((sum, d) => sum + d.kwh, 0);
  if (devs.length && total - tracked > total * 0.02) devs.push({ name: 'Autres', kwh: total - tracked, other: true });

  return { period, slots, total, prevTotal: hasPrev ? prevTotal : null, devs, power, meters: src.meters };
}

/* Normalise une série de statistiques HA en [{t, change}] (format ms ou ISO, avec ou sans « change »). */
function changes(rows) {
  if (!rows) return [];
  let prevSum = null;
  return rows.map(r => {
    const t = typeof r.start === 'number' ? r.start : Date.parse(r.start);
    let change = r.change;
    if (change == null && r.sum != null) { change = prevSum == null ? 0 : r.sum - prevSum; }
    if (r.sum != null) prevSum = r.sum;
    return { t, change: Math.max(0, Number(change) || 0) };
  });
}

async function fetchPower(id, now) {
  if (!id || !backend.history) return null;
  const e = S.entities[id];
  const unit = e?.attributes?.unit_of_measurement;
  let res;
  try { res = await backend.history([id], new Date(now - DAY), new Date(now)); } catch { return null; }
  const pts = (res?.[id] || []).map(p => ({
    t: p.lu != null ? p.lu * 1000 : Date.parse(p.last_updated || p.last_changed),
    w: toWatts(Number(p.s ?? p.state), unit),
  })).filter(p => Number.isFinite(p.w) && Number.isFinite(p.t)).sort((a, b) => a.t - b.t);
  if (e && !isUnavailable(e)) pts.push({ t: now, w: toWatts(Number(e.state), unit) });
  if (pts.length < 2) return null;
  return { id, name: e ? nameOf(e) : id, pts, t0: now - DAY, t1: now };
}

/* ---------- Mise en page ---------- */

function chartWidth() {
  const v = document.getElementById('view');
  const cs = getComputedStyle(v);
  const inner = v.clientWidth - parseFloat(cs.paddingLeft) - parseFloat(cs.paddingRight);
  return Math.max(260, Math.round(Math.min(inner, 980) - 2 * 18 - 2));
}

function niceTicks(max, count = 4) {
  if (!(max > 0)) max = 1;
  const raw = max / count;
  const mag = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map(m => m * mag).find(s => s >= raw);
  const top = Math.ceil(max / step - 1e-9) * step;
  const ticks = [];
  for (let v = 0; v <= top + step / 2; v += step) ticks.push(Math.round(v * 1e6) / 1e6);
  return { top, ticks, step };
}

function fmtKwhPrecise(v) { return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: v < 1 ? 2 : 1 }).format(v) + ' kWh'; }
function fmtW(w) { return w >= 1000 ? fmtNum(w / 1000) + ' kW' : fmtNum(w, 0) + ' W'; }
function fmtEur(v) { return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: v < 10 ? 2 : 0 }).format(v); }
function fmtTickW(v) { return v >= 10000 ? fmtNum(v / 1000, 0) + ' k' : fmtNum(v, 0); }
function fmtSlot(t, stat) {
  const d = new Date(t);
  if (stat === 'hour') return `${d.getHours()} h – ${d.getHours() + 1} h`;
  return d.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
}

/* Barre à bout arrondi (4 px), carrée sur la ligne de base. */
function barPath(x, base, w, h) {
  if (h <= 0.5) return '';
  const r = Math.min(4, w / 2, h);
  return `M${x},${base}V${base - h + r}A${r},${r} 0 0 1 ${x + r},${base - h}H${x + w - r}A${r},${r} 0 0 1 ${x + w},${base - h + r}V${base}Z`;
}

function columnChart(data, W) {
  const cfg = PERIODS[data.period];
  const slots = data.slots;
  const H = 210, m = { l: 40, r: 8, t: 22, b: 26 };
  const pw = W - m.l - m.r, ph = H - m.t - m.b, base = m.t + ph;
  const max = Math.max(...slots.map(s => s.kwh));
  const { top, ticks } = niceTicks(max, 4);
  const y = v => base - (v / top) * ph;
  const slotW = pw / slots.length;
  const bw = Math.max(2, Math.min(24, slotW * 0.72, slotW - 2));
  const price = Number(S.settings.price) || 0;
  const peak = slots.reduce((best, s, i) => (s.kwh > (slots[best]?.kwh ?? -1) ? i : best), -1);
  let out = `<svg class="chart" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="${esc(cfg.chart)} : ${esc(fmtKwhPrecise(data.total))} au total">`;
  for (const v of ticks) {
    out += `<line class="grid" x1="${m.l}" x2="${W - m.r}" y1="${y(v)}" y2="${y(v)}"/>`;
    out += `<text class="tick" x="${m.l - 6}" y="${y(v) + 4}" text-anchor="end">${fmtNum(v, top < 2 ? 1 : 0)}</text>`;
  }
  slots.forEach((s, i) => {
    const x = m.l + i * slotW + (slotW - bw) / 2;
    const h = (s.kwh / top) * ph;
    if (!s.future) out += `<path class="bar${s.partial ? ' partial' : ''}" data-i="${i}" d="${barPath(x, base, bw, h)}"/>`;
    // étiquettes de l'axe des X, espacées pour ne pas se chevaucher
    const every = cfg.stat === 'hour' ? 6 : slots.length > 10 ? 5 : 1;
    if (i % every === 0) {
      const d = new Date(s.t);
      const lbl = cfg.stat === 'hour' ? `${d.getHours()} h` : slots.length > 10 ? d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) : d.toLocaleDateString('fr-FR', { weekday: 'short' }).replace('.', '');
      out += `<text class="tick" x="${m.l + i * slotW + slotW / 2}" y="${H - 7}" text-anchor="middle">${esc(lbl)}</text>`;
    }
    if (!s.future) {
      const tip = `${fmtSlot(s.t, cfg.stat)}${s.partial ? ' (en cours)' : ''}|${s.has ? fmtKwhPrecise(s.kwh) + (price ? ' · ' + fmtEur(s.kwh * price) : '') : 'Pas encore de relevé'}`;
      out += `<rect class="hit" x="${m.l + i * slotW}" y="${m.t}" width="${slotW}" height="${ph}" data-i="${i}" data-tip="${esc(tip)}"/>`;
    }
  });
  // une seule valeur écrite sur le graphique : le pic
  if (peak >= 0 && slots[peak].kwh > 0) {
    const cx = Math.min(W - m.r - 24, Math.max(m.l + 24, m.l + peak * slotW + slotW / 2));
    out += `<text class="val" x="${cx}" y="${y(slots[peak].kwh) - 7}" text-anchor="middle">${esc(fmtKwhPrecise(slots[peak].kwh))}</text>`;
  }
  out += `<line class="axis" x1="${m.l}" x2="${W - m.r}" y1="${base}" y2="${base}"/></svg>`;
  return out;
}

function lineChart(power, W) {
  const H = 200, m = { l: 44, r: 10, t: 24, b: 26 };
  const pw = W - m.l - m.r, ph = H - m.t - m.b, base = m.t + ph;
  const { t0, t1, pts } = power;
  // une valeur par tranche de 2 px : moyenne des relevés de la tranche, sinon la dernière valeur connue
  const n = Math.max(20, Math.floor(pw / 2));
  const step = (t1 - t0) / n;
  const vals = [], times = [];
  let j = 0, last = null;
  while (j < pts.length && pts[j].t < t0) last = pts[j++].w;
  for (let i = 0; i < n; i++) {
    const end = t0 + (i + 1) * step;
    let sum = 0, cnt = 0;
    while (j < pts.length && pts[j].t <= end) { sum += pts[j].w; cnt++; last = pts[j++].w; }
    vals.push(cnt ? sum / cnt : last);
    times.push(end);
  }
  const known = vals.filter(v => v != null);
  const max = Math.max(...known, 1);
  const { top, ticks } = niceTicks(max, 4);
  const x = i => m.l + (i + 0.5) * (pw / n);
  const y = v => base - (v / top) * ph;
  let line = '', first = -1, lastI = -1;
  vals.forEach((v, i) => {
    if (v == null) return;
    line += `${first < 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(v).toFixed(1)}`;
    if (first < 0) first = i;
    lastI = i;
  });
  const area = `${line}L${x(lastI).toFixed(1)},${base}L${x(first).toFixed(1)},${base}Z`;
  const peakI = vals.reduce((b, v, i) => (v != null && v > (vals[b] ?? -1) ? i : b), first);
  const id = 'pw' + W;
  LINE_CHARTS[id] = { vals, times, x0: m.l, pw, n, top, base, ph, W, H };

  let out = `<svg class="chart line" id="${id}" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="Puissance sur 24 heures, maximum ${esc(fmtW(vals[peakI]))}">`;
  for (const v of ticks) {
    out += `<line class="grid" x1="${m.l}" x2="${W - m.r}" y1="${y(v)}" y2="${y(v)}"/>`;
    out += `<text class="tick" x="${m.l - 6}" y="${y(v) + 4}" text-anchor="end">${fmtTickW(v)}</text>`;
  }
  // heures pleines toutes les 6 h
  const firstHour = Math.ceil(t0 / 3600e3) * 3600e3;
  for (let t = firstHour; t <= t1; t += 3600e3) {
    if (new Date(t).getHours() % 6) continue;
    const px = m.l + ((t - t0) / (t1 - t0)) * pw;
    if (px < m.l + 14 || px > W - m.r - 14) continue;
    out += `<text class="tick" x="${px}" y="${H - 7}" text-anchor="middle">${new Date(t).getHours()} h</text>`;
  }
  out += `<path class="area" d="${area}"/><path class="line" d="${line}"/>`;
  out += `<line class="axis" x1="${m.l}" x2="${W - m.r}" y1="${base}" y2="${base}"/>`;
  // étiquettes sélectives : le pic et la valeur actuelle
  const px = x(peakI), py = y(vals[peakI]);
  const anchor = px > W - 90 ? 'end' : px < m.l + 60 ? 'start' : 'middle';
  out += `<circle class="dot" cx="${px}" cy="${py}" r="4"/><text class="val" x="${px}" y="${Math.max(12, py - 9)}" text-anchor="${anchor}">Pic ${esc(fmtW(vals[peakI]))}</text>`;
  if (Math.abs(lastI - peakI) > n * 0.12) {
    out += `<circle class="dot" cx="${x(lastI)}" cy="${y(vals[lastI])}" r="4"/><text class="val" x="${x(lastI) - 8}" y="${Math.max(12, y(vals[lastI]) - 9)}" text-anchor="end">${esc(fmtW(vals[lastI]))}</text>`;
  }
  out += `<line class="xhair" x1="0" x2="0" y1="${m.t}" y2="${base}" visibility="hidden"/><circle class="xdot" r="4" cx="0" cy="0" visibility="hidden"/>`;
  out += `<rect class="hit-area" x="${m.l}" y="${m.t}" width="${pw}" height="${ph}" data-line="${id}"/></svg>`;
  return out;
}

function breakdown(devs, total) {
  const max = Math.max(...devs.map(d => d.kwh));
  return `<div class="breakdown">${devs.map(d => `
    <div class="bd-row${d.other ? ' other' : ''}">
      <span class="bd-name">${esc(d.name)}</span>
      <span class="bd-track"><span class="bd-bar" style="width:${Math.max(1, (d.kwh / max) * 100).toFixed(1)}%"></span></span>
      <span class="bd-val">${esc(fmtKwhPrecise(d.kwh))}<small>${total > 0 ? fmtNum((d.kwh / total) * 100, 0) + ' %' : ''}</small></span>
    </div>`).join('')}</div>`;
}

function dataTable(head, rows) {
  return `<details class="data-table"><summary>Voir les données</summary><div class="table-wrap"><table>
    <thead><tr>${head.map(h => `<th>${esc(h)}</th>`).join('')}</tr></thead>
    <tbody>${rows.map(r => `<tr>${r.map(c => `<td>${esc(c)}</td>`).join('')}</tr>`).join('')}</tbody></table></div></details>`;
}

/* ---------- Vue ---------- */

function viewEnergy() {
  loadEnergy();
  const E = S.energy, cfg = PERIODS[E.period];
  const d = E.data && E.data.period === E.period ? E.data : null;
  const price = Number(S.settings.price) || 0;
  const W = chartWidth();
  const filters = `<div class="segmented" role="group" aria-label="Période">${Object.entries(PERIODS).map(([k, p]) =>
    `<button class="${k === E.period ? 'sel' : ''}" aria-pressed="${k === E.period}" data-act="energy-period" data-period="${k}">${p.label}</button>`).join('')}</div>`;
  const head = `<div class="room-head"><h1 class="page-title">Énergie</h1>${filters}</div>`;

  if (!d) {
    if (E.error) return `${head}<div class="empty">${icon('alert')}<p>Impossible de charger la consommation : ${esc(E.error)}</p><p><button class="btn" data-act="energy-reload">Réessayer</button></p></div>`;
    return `${head}<div class="empty big"><div class="spinner"></div><p>Chargement de la consommation…</p></div>`;
  }

  const powerCard = d.power ? `<section class="card chart-card">
      <h2>Puissance sur 24 h</h2><p class="chart-sub">En watts · ${esc(d.power.name)}</p>
      <div class="chart-box">${lineChart(d.power, W)}</div>
      ${dataTable(['Heure', 'Puissance moyenne'], hourlyPower(d.power).map(r => [r.label, fmtW(r.w)]))}
    </section>` : '';

  if (d.empty) {
    return `${head}<div class="empty">${icon('bolt')}<p>Aucun compteur d’énergie trouvé. Dans Home Assistant, configure le tableau de bord <b>Énergie</b> (Paramètres → Tableaux de bord → Énergie) avec ton compteur, ou choisis un capteur en kWh dans les Réglages.</p></div>${powerCard}`;
  }

  const delta = d.prevTotal ? (d.total - d.prevTotal) / d.prevTotal : null;
  const now = Date.now();
  const powerNow = d.power ? d.power.pts[d.power.pts.length - 1].w : null;
  const powerPeak = d.power ? Math.max(...d.power.pts.filter(p => p.t >= now - DAY).map(p => p.w)) : null;
  const kpis = [
    `<div class="stat"><span class="stat-ic power">${icon('bolt')}</span><span><b>${esc(fmtKwhPrecise(d.total))}</b><small>Consommé · ${esc(cfg.label.toLowerCase())}</small></span></div>`,
    price ? `<div class="stat"><span class="stat-ic">${icon('gauge')}</span><span><b>${esc(fmtEur(d.total * price))}</b><small>Coût estimé · ${esc(String(price).replace('.', ','))} €/kWh</small></span></div>` : '',
    delta == null ? '' : Math.abs(delta) < 0.005
      ? `<div class="stat"><span class="stat-ic">${icon('minus')}</span><span><b>Stable</b><small>${esc(cfg.prev)}</small></span></div>`
      : `<div class="stat"><span class="stat-ic ${delta > 0 ? 'bad' : 'ok'}">${icon(delta > 0 ? 'up' : 'down')}</span><span><b class="${delta > 0 ? 'txt-bad' : 'txt-ok'}">${delta > 0 ? '+' : '−'}${fmtNum(Math.abs(delta) * 100, 0)} %</b><small>${delta > 0 ? 'En hausse' : 'En baisse'} ${esc(cfg.prev)}</small></span></div>`,
    powerNow != null ? `<div class="stat"><span class="stat-ic light">${icon('plugBolt')}</span><span><b>${esc(fmtW(powerNow))}</b><small>Maintenant · pic 24 h ${esc(fmtW(powerPeak))}</small></span></div>` : '',
  ].join('');

  const columns = `<section class="card chart-card">
      <h2>${esc(cfg.chart)}</h2><p class="chart-sub">En kWh · ${esc(cfg.label.toLowerCase())}${d.slots.some(s => s.partial && s.has) ? ' · la barre claire est en cours' : ''}</p>
      <div class="chart-box">${columnChart(d, W)}</div>
      ${dataTable([cfg.stat === 'hour' ? 'Heure' : 'Jour', 'Consommation', ...(price ? ['Coût'] : [])],
        d.slots.filter(s => !s.future).map(s => [fmtSlot(s.t, cfg.stat), s.has ? fmtKwhPrecise(s.kwh) : '—', ...(price ? [s.has ? fmtEur(s.kwh * price) : '—'] : [])]))}
    </section>`;

  const devCard = d.devs.length ? `<section class="card chart-card">
      <h2>Répartition par appareil</h2><p class="chart-sub">En kWh · ${esc(cfg.label.toLowerCase())}</p>
      ${breakdown(d.devs, d.total)}
      ${price ? `<p class="chart-sub">Le plus gros poste, ${esc(d.devs[0].name.toLowerCase())}, représente environ ${esc(fmtEur(d.devs[0].kwh * price))}.</p>` : ''}
    </section>` : '';

  return `${head}
    <div class="stats energy-stats">${kpis}</div>
    ${columns}
    ${powerCard}
    ${devCard}
    <p class="muted small-print">Données ${backend?.kind === 'demo' ? 'simulées (maison de démo)' : 'issues des statistiques de Home Assistant, mises à jour chaque heure'}. ${E.at ? 'Actualisé ' + relTime(new Date(E.at).toISOString()) + '.' : ''} <button class="link-btn" data-act="energy-reload">Actualiser</button></p>`;
}

function hourlyPower(power) {
  const rows = [];
  for (let t = Math.floor(power.t0 / 3600e3) * 3600e3; t < power.t1; t += 3600e3) {
    const inHour = power.pts.filter(p => p.t >= t && p.t < t + 3600e3);
    if (!inHour.length) continue;
    rows.push({ label: `${new Date(t).getHours()} h`, w: inHour.reduce((s, p) => s + p.w, 0) / inHour.length });
  }
  return rows;
}

/* ---------- Survol (info-bulle) ---------- */

function showTip(html, clientX, clientY) {
  const tip = document.getElementById('chartTip');
  tip.innerHTML = html;
  tip.hidden = false;
  const r = tip.getBoundingClientRect();
  let left = clientX - r.width / 2;
  left = Math.max(8, Math.min(window.innerWidth - r.width - 8, left));
  let top = clientY - r.height - 14;
  if (top < 8) top = clientY + 18;
  tip.style.left = left + 'px';
  tip.style.top = top + 'px';
}

function hideTip() {
  document.getElementById('chartTip').hidden = true;
  document.querySelectorAll('.chart .xhair, .chart .xdot').forEach(el => el.setAttribute('visibility', 'hidden'));
  document.querySelectorAll('.chart .bar.hover').forEach(el => el.classList.remove('hover'));
}

function onChartPointer(ev) {
  const hit = ev.target.closest?.('[data-tip]');
  if (hit) {
    const [a, b] = hit.dataset.tip.split('|');
    document.querySelectorAll('.chart .bar.hover').forEach(el => el.classList.remove('hover'));
    hit.parentNode.querySelector(`.bar[data-i="${hit.dataset.i}"]`)?.classList.add('hover');
    showTip(`<small>${esc(a)}</small><b>${esc(b)}</b>`, ev.clientX, ev.clientY);
    return;
  }
  const area = ev.target.closest?.('[data-line]');
  if (area) {
    const c = LINE_CHARTS[area.dataset.line];
    const svg = area.ownerSVGElement;
    if (!c || !svg) return;
    const rect = svg.getBoundingClientRect();
    const sx = (ev.clientX - rect.left) * (c.W / rect.width);
    let i = Math.round((sx - c.x0) / (c.pw / c.n) - 0.5);
    i = Math.max(0, Math.min(c.n - 1, i));
    while (i > 0 && c.vals[i] == null) i--;
    const v = c.vals[i];
    if (v == null) return;
    const px = c.x0 + (i + 0.5) * (c.pw / c.n), py = c.base - (v / c.top) * c.ph;
    const xh = svg.querySelector('.xhair'), xd = svg.querySelector('.xdot');
    xh.setAttribute('x1', px); xh.setAttribute('x2', px); xh.setAttribute('visibility', 'visible');
    xd.setAttribute('cx', px); xd.setAttribute('cy', py); xd.setAttribute('visibility', 'visible');
    const time = new Date(c.times[i]).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    showTip(`<small>${time}</small><b>${esc(fmtW(v))}</b>`, rect.left + px * (rect.width / c.W), rect.top + py * (rect.height / c.H));
    return;
  }
  if (!document.getElementById('chartTip').hidden) hideTip();
}

document.addEventListener('pointermove', onChartPointer);
document.addEventListener('pointerdown', ev => { if (ev.target.closest?.('.chart')) onChartPointer(ev); });
window.addEventListener('scroll', () => { if (!document.getElementById('chartTip').hidden) hideTip(); }, { passive: true });
