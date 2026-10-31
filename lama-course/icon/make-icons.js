// Génère l'icône (lama qui court) et l'écran de démarrage de Lama Course, aux tailles de l'APK.
// Le dessin réutilise les lamas coureurs du jeu (index.html). Usage : node make-icons.js
// Sortie : icon/res/... (mêmes chemins que dans l'APK), insérés par build.sh. Nécessite Python 3 avec Pillow.
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
const D = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };
const SPLASH = { mdpi: [320, 480], hdpi: [480, 800], xhdpi: [720, 1280], xxhdpi: [960, 1600], xxxhdpi: [1280, 1920] };
const specs = [];
for (const [d, k] of Object.entries(D)) {
  specs.push({ out: `res/mipmap-${d}-v4/ic_launcher_foreground.png`, kind: 'full', w: 108 * k, h: 108 * k });
  specs.push({ out: `res/mipmap-${d}-v4/ic_launcher.png`, kind: 'square', w: 48 * k, h: 48 * k });
  specs.push({ out: `res/mipmap-${d}-v4/ic_launcher_round.png`, kind: 'round', w: 48 * k, h: 48 * k });
  const [w, h] = SPLASH[d];
  specs.push({ out: `res/drawable-port-${d}-v4/splash.png`, kind: 'splash', w, h });
  specs.push({ out: `res/drawable-land-${d}-v4/splash.png`, kind: 'splash', w: h, h: w });
}
specs.push({ out: 'res/drawable/splash.png', kind: 'splash', w: 320, h: 480 });

(async () => {
  const b = await chromium.launch(); const p = await b.newPage();
  await p.goto('file://' + path.join(__dirname, '..', 'index.html')); await p.waitForTimeout(400);
  const imgs = await p.evaluate(specs => {
    const C = RACERS[ME], legFar = sprite(() => racerLeg(C, true), -7, -2, 14, 35, 24);
    function llama(x, y, s) {   // lama rose en plein galop, pattes tendues
      ctx.save(); ctx.translate(x, y); ctx.scale(s, s);
      softShadow(0, 0, 26, 4, .35);
      const bob = -3;
      racerLegAt(legFar, -14, -31 + bob, .5); racerLegAt(legFar, 13, -31 + bob, -.45);
      ctx.save(); ctx.translate(0, bob); racerBody(C, true); ctx.restore();
      for (const [lx, a] of [[-17, .85], [16, -.9]]) { ctx.save(); ctx.translate(lx, -30 + bob); ctx.rotate(a); racerLeg(C, false); ctx.restore(); }
      ctx.save(); ctx.translate(14, -45 + bob); ctx.rotate(.22); racerHead(C); ctx.restore();
      ctx.restore();
    }
    function scene() {   // repère 108 × 108 (icône adaptative Android)
      ctx.fillStyle = vgrad(0, 76, [[0, '#3a68cc'], [.5, '#b076c4'], [1, '#ffa45e']]); ctx.fillRect(0, 0, 108, 108);
      ctx.fillStyle = rg(76, 60, 2, 76, 60, 34, [[0, 'rgba(255,240,180,.9)'], [.4, 'rgba(255,200,110,.45)'], [1, 'rgba(255,170,90,0)']]); ctx.fillRect(0, 0, 108, 108);
      ctx.fillStyle = rg(74, 58, 1, 76, 60, 11, [[0, '#fffbe0'], [1, '#ffd060']]); circ(76, 60, 10);
      ctx.fillStyle = lg(0, 52, 0, 78, [[0, '#8a5a98'], [1, '#5a3a68']]);
      ctx.beginPath(); ctx.moveTo(0, 70); ctx.lineTo(22, 57); ctx.lineTo(38, 66); ctx.lineTo(58, 53); ctx.lineTo(80, 67); ctx.lineTo(96, 59); ctx.lineTo(108, 65); ctx.lineTo(108, 80); ctx.lineTo(0, 80); ctx.fill();
      ctx.fillStyle = vgrad(75, 108, [[0, '#f6cf8a'], [1, '#d89a50']]); ctx.fillRect(0, 75, 108, 33);
      ctx.fillStyle = 'rgba(255,255,255,.55)'; ctx.fillRect(0, 75, 108, 1.2);
      seed = 5; for (let i = 0; i < 90; i++) { ctx.fillStyle = rnd() < .5 ? 'rgba(120,60,20,.18)' : 'rgba(255,240,200,.35)'; circ(rnd() * 108, 77 + rnd() * 31, .3 + rnd() * .7); }
      ctx.lineCap = 'round'; ctx.strokeStyle = 'rgba(255,255,255,.9)';   // traits de vitesse
      for (const [x0, x1, y, w] of [[20, 35, 46, 2.4], [15, 32, 56, 2.4], [21, 34, 66, 2]]) { ctx.lineWidth = w; ctx.beginPath(); ctx.moveTo(x0, y); ctx.lineTo(x1, y); ctx.stroke(); }
      for (const [x, y, r, a] of [[33, 84, 4, .6], [27, 81.5, 3.2, .5], [22, 83.5, 2.4, .4], [38, 86, 2.8, .55]]) { ctx.fillStyle = `rgba(236,200,146,${a})`; circ(x, y, r); }
      llama(54, 86, .6);
    }
    const out = {};
    for (const s of specs) {
      const c = document.createElement('canvas'); c.width = s.w; c.height = s.h;
      const prev = ctx; ctx = c.getContext('2d');
      if (s.kind === 'full') { ctx.scale(s.w / 108, s.w / 108); scene(); }
      else if (s.kind === 'square' || s.kind === 'round') {   // anciennes icônes : la zone utile (14..94) remplit l'icône
        ctx.scale(s.w / 80, s.w / 80); ctx.translate(-14, -14);
        const shape = () => { if (s.kind === 'round') { ctx.beginPath(); ctx.arc(54, 54, 39, 0, 6.283); } else rr(15, 15, 78, 78, 13); };
        ctx.save(); shape(); ctx.clip(); scene(); ctx.restore();
        ctx.strokeStyle = 'rgba(0,0,0,.18)'; ctx.lineWidth = .8; shape(); ctx.stroke();
      } else {   // écran de démarrage : icône ronde et titre sur le violet du stand
        ctx.fillStyle = rg(s.w / 2, s.h * .45, 10, s.w / 2, s.h * .45, Math.max(s.w, s.h) * .7, [[0, '#3a1850'], [1, '#120a1f']]); ctx.fillRect(0, 0, s.w, s.h);
        const m = Math.min(s.w, s.h), r = m * .26, cx = s.w / 2, cy = s.h * .44;
        ctx.save(); ctx.translate(cx, cy); ctx.scale(r / 39, r / 39); ctx.translate(-54, -54);
        softShadow(54, 96, 34, 6, .5);
        ctx.save(); ctx.beginPath(); ctx.arc(54, 54, 39, 0, 6.283); ctx.clip(); scene(); ctx.restore();
        ctx.strokeStyle = '#ffe08a'; ctx.lineWidth = 2.2; ctx.beginPath(); ctx.arc(54, 54, 39, 0, 6.283); ctx.stroke();
        ctx.restore();
        ctx.save(); ctx.translate(cx, cy + r + m * .1); ctx.scale(m / 540, m / 540); text('LAMA COURSE', 0, 0, 52, '#fff', 'center', '#7a2230'); ctx.restore();
      }
      ctx = prev; out[s.out] = c.toDataURL('image/png');
    }
    return out;
  }, specs);
  for (const [f, url] of Object.entries(imgs)) {
    const dst = path.join(__dirname, f); fs.mkdirSync(path.dirname(dst), { recursive: true });
    fs.writeFileSync(dst, Buffer.from(url.split(',')[1], 'base64'));
  }
  // écrans de démarrage : opaques, on retire la couche alpha et on optimise la compression (sans perte)
  const splashes = specs.filter(s => s.kind === 'splash').map(s => path.join(__dirname, s.out));
  const py = "import sys\nfrom PIL import Image\nfor f in sys.argv[1:]:\n  Image.open(f).convert('RGB').save(f, optimize=True)";
  const r = require('child_process').spawnSync('python3', ['-I', '-c', py, ...splashes], { stdio: 'inherit' });
  if (r.status) throw new Error('optimisation des écrans de démarrage impossible');
  console.log(Object.keys(imgs).length, 'images');
  await b.close();
})();
