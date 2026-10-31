(() => {
const $ = s => document.querySelector(s), audio = $('#audio');
const store = {get:(k,d)=>{try{return JSON.parse(localStorage.getItem(k))??d}catch{return d}},set:(k,v)=>{try{localStorage.setItem(k,JSON.stringify(v))}catch{}}};
const API = ['https://de1.api.radio-browser.info','https://fr1.api.radio-browser.info','https://nl1.api.radio-browser.info'];
let stations = [...STATIONS], remote = [], tab = 'home', genre = 'Tous', query = '';
let favs = store.get('favs', []), cur = null, queue = [], sleepT = null;

const esc = s => String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const initials = n => n.replace(/[^\p{L}\p{N} ]/gu,'').split(' ').filter(Boolean).slice(0,2).map(w=>w[0]).join('').toUpperCase() || '♪';
const color = n => { let h=0; for (const c of n) h=(h*31+c.charCodeAt(0))%360; return `hsl(${h} 65% 48%)`; };
function logo(s, cls=''){
  const bg = s.icon ? `background-image:url("${esc(s.icon)}");background-color:var(--card2)` : `background:linear-gradient(135deg,${s.c||color(s.name)},${color(s.name+'x')})`;
  return `<div class="logo ${cls}" style="${bg}">${s.icon?'':esc(initials(s.name))}</div>`;
}
const all = () => [...stations, ...remote.filter(r => !stations.some(s => s.url === r.url))];
const isFav = s => favs.includes(s.id);

// ---- Rendu ----
const genres = () => ['Tous', ...new Set(stations.map(s => s.genre))];
function renderGenres(){
  $('#genres').innerHTML = genres().map(g => `<button class="${g===genre?'on':''}" data-g="${esc(g)}">${esc(g)}</button>`).join('');
}
const card = s => `<button class="card ${cur&&cur.id===s.id?'now':''}" data-id="${esc(s.id)}">${logo(s)}<div><b>${esc(s.name)}</b><small>${esc(s.genre||'Radio')}</small></div></button>`;
const item = s => `<button class="row-item" data-id="${esc(s.id)}">${logo(s)}<div><b>${esc(s.name)}</b><small>${esc(s.genre||'Radio')}</small></div></button>`;
function render(){
  renderGenres();
  const v = $('#view'), q = query.toLowerCase();
  let list = all().filter(s => (genre==='Tous' || s.genre===genre) && (!q || (s.name+' '+(s.genre||'')).toLowerCase().includes(q)));
  if (q) { v.innerHTML = list.length ? `<h3>Résultats</h3><div class="list">${list.map(item).join('')}</div>` : '<p class="empty">Aucune radio trouvée.</p>'; return; }
  if (tab==='fav') { const f = all().filter(isFav); v.innerHTML = f.length ? `<h3>Mes favoris</h3><div class="grid">${f.map(card).join('')}</div>` : '<p class="empty">Aucun favori pour l’instant.<br>Ouvrez une radio puis touchez ♥.</p>'; return; }
  if (tab==='all') { v.innerHTML = `<h3>Toutes les radios</h3><div class="list">${list.map(item).join('')}</div>`; return; }
  const recent = store.get('recent', []).map(id => all().find(s => s.id===id)).filter(Boolean);
  v.innerHTML = (recent.length && genre==='Tous' ? `<h3>Écoutées récemment</h3><div class="grid">${recent.slice(0,4).map(card).join('')}</div>` : '')
    + `<h3>${genre==='Tous'?'À la une':esc(genre)}</h3><div class="grid">${list.map(card).join('')}</div>`;
}

// ---- Lecture ----
const PLAY = '<svg viewBox="0 0 24 24"><path d="M7 4v16l13-8z"/></svg>', PAUSE = '<svg viewBox="0 0 24 24"><path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg>', LOAD = '<svg class="spin" viewBox="0 0 24 24" style="fill:none;stroke:currentColor;stroke-width:3"><path d="M12 3a9 9 0 1 0 9 9"/></svg>';
function ui(state){
  const ic = state==='load' ? LOAD : state==='play' ? PAUSE : PLAY;
  $('#miniPlay').innerHTML = ic; $('#fullPlay').innerHTML = ic;
  $('#viz').classList.toggle('on', state==='play'); $('#fullLogo').classList.toggle('play', state==='play');
  $('#fullSub').textContent = state==='load' ? 'Connexion…' : state==='err' ? 'Flux indisponible — essayez une autre radio' : (cur.genre || 'En direct');
  $('#miniSub').textContent = $('#fullSub').textContent;
}
function play(s, list){
  cur = s; if (list) queue = list;
  audio.src = s.url; audio.play().catch(()=>ui('err'));
  $('#mini').classList.remove('hidden');
  $('#miniName').textContent = $('#fullName').textContent = s.name;
  $('#miniLogo').outerHTML = logo(s).replace('class="logo ','id="miniLogo" class="logo ');
  $('#fullLogo').outerHTML = logo(s).replace('class="logo ','id="fullLogo" class="art logo ');
  $('#full').style.setProperty('--glow', (s.c || color(s.name)) + '66');
  $('#favBtn').classList.toggle('on', isFav(s));
  store.set('recent', [s.id, ...store.get('recent', []).filter(i => i!==s.id)].slice(0,8));
  ui('load'); media(); render();
}
function step(d){ const l = queue.length ? queue : stations; const i = l.findIndex(x => x.id===cur?.id); play(l[(i+d+l.length)%l.length]); }
function toggle(){ if (!cur) return; audio.paused ? audio.play().catch(()=>ui('err')) : audio.pause(); }
audio.addEventListener('playing', () => ui('play'));
audio.addEventListener('pause', () => cur && ui('pause'));
audio.addEventListener('waiting', () => cur && ui('load'));
audio.addEventListener('error', () => cur && ui('err'));
function media(){
  if (!('mediaSession' in navigator)) return;
  navigator.mediaSession.metadata = new MediaMetadata({title: cur.name, artist: cur.genre || 'En direct', album: 'Onde', artwork: cur.icon ? [{src: cur.icon}] : [{src:'icon.svg', sizes:'512x512', type:'image/svg+xml'}]});
  navigator.mediaSession.setActionHandler('play', toggle); navigator.mediaSession.setActionHandler('pause', toggle);
  navigator.mediaSession.setActionHandler('previoustrack', () => step(-1)); navigator.mediaSession.setActionHandler('nexttrack', () => step(1));
}

// ---- Minuteur ----
function sleep(){
  const opts = [0,15,30,60,90]; const cd = sleepT ? sleepT.m : 0;
  const m = opts[(opts.indexOf(cd)+1) % opts.length];
  clearTimeout(sleepT?.t); sleepT = null; $('#sleepLbl').textContent = '';
  if (m) { sleepT = {m, t: setTimeout(() => { audio.pause(); sleepT = null; $('#sleepLbl').textContent=''; }, m*60000)}; $('#sleepLbl').textContent = m + ' min'; }
}

// ---- Recherche en ligne (API ouverte Radio Browser), repli sur le catalogue ----
let deb;
async function remoteSearch(q){
  for (const base of API) {
    try {
      const r = await fetch(`${base}/json/stations/search?countrycode=FR&hidebroken=true&order=clickcount&reverse=true&limit=40&name=${encodeURIComponent(q)}`, {signal: AbortSignal.timeout(5000)});
      const j = await r.json();
      remote = j.filter(x => x.url_resolved).map(x => ({id:'rb'+x.stationuuid, name:x.name.trim(), genre:(x.tags||'').split(',')[0] || 'Radio', url:x.url_resolved, icon:x.favicon && x.favicon.startsWith('https') ? x.favicon : ''}));
      return render();
    } catch {}
  }
}

// ---- Événements ----
$('#view').addEventListener('click', e => { const b = e.target.closest('[data-id]'); if (!b) return; const s = all().find(x => x.id===b.dataset.id); if (s) play(s, all()); });
$('#genres').addEventListener('click', e => { const g = e.target.closest('[data-g]'); if (g) { genre = g.dataset.g; render(); } });
document.querySelectorAll('.tabs button').forEach(b => b.onclick = () => { document.querySelectorAll('.tabs button').forEach(x => x.classList.toggle('on', x===b)); tab = b.dataset.tab; $('#q').value = query = ''; render(); scrollTo(0,0); });
$('#q').addEventListener('input', e => { query = e.target.value.trim(); render(); clearTimeout(deb); if (query.length > 2) deb = setTimeout(() => remoteSearch(query), 400); else { remote = []; } });
$('#miniPlay').onclick = $('#fullPlay').onclick = toggle;
$('#prev').onclick = () => step(-1); $('#next').onclick = () => step(1);
$('#openFull').onclick = () => $('#full').classList.remove('hidden');
$('#closeFull').onclick = () => $('#full').classList.add('hidden');
$('#vol').oninput = e => audio.volume = e.target.value;
$('#sleepBtn').onclick = sleep;
$('#favBtn').onclick = () => { if (!cur) return; favs = isFav(cur) ? favs.filter(i => i!==cur.id) : [...favs, cur.id]; store.set('favs', favs); $('#favBtn').classList.toggle('on', isFav(cur)); render(); };
// Les favoris issus de la recherche en ligne sont conservés pour la session suivante
const saved = store.get('favStations', []); stations.push(...saved.filter(s => !stations.some(x => x.id===s.id)));
addEventListener('beforeunload', () => store.set('favStations', all().filter(s => isFav(s) && s.id.startsWith('rb'))));
if ('serviceWorker' in navigator && location.protocol.startsWith('http')) navigator.serviceWorker.register('sw.js').catch(()=>{});
render();
})();
