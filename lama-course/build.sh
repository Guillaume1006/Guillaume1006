#!/usr/bin/env bash
# Construit LamaCourse.apk à partir de l'APK Capacitor de Lama Splash :
# - remplace le jeu (assets/public/index.html)
# - change l'identifiant (com.lamacourse.game) et le nom (« Lama Course ») pour qu'il s'installe à côté de Lama Splash.
#   Les noms ont la même longueur, on corrige donc directement le manifeste et la table de ressources binaires.
# Prérequis (Ubuntu) : apt install apksigner zipalign
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
BASE="${1:-$HERE/../lama-splash/LamaSplash.apk}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

python3 -I - "$BASE" "$HERE/index.html" "$OUT/unsigned.apk" <<'PY'
import sys, zipfile
src, page, dst = sys.argv[1:]
u = lambda s: s.encode('utf-16-le')
def sub(buf, a, b):
    assert len(a) == len(b) and buf.count(a) == 1, (buf.count(a), a[:40])
    return buf.replace(a, b)
def manifest(m):
    for suf in ['.fileprovider', '.androidx-startup', '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION']:
        m = sub(m, u('com.lamasplash.game' + suf), u('com.lamacourse.game' + suf))
    # identifiant de l'application ; la classe com.lamasplash.game.MainActivity garde son nom
    return sub(m, b'\x13\x00' + u('com.lamasplash.game') + b'\x00\x00', b'\x13\x00' + u('com.lamacourse.game') + b'\x00\x00')
patch = {
    'AndroidManifest.xml': manifest,
    'resources.arsc': lambda r: sub(r, b'Lama Splash', b'Lama Course'),
    'assets/capacitor.config.json': lambda c: c.replace(b'com.lamasplash.game', b'com.lamacourse.game').replace(b'Lama Splash', b'Lama Course'),
    'assets/public/index.html': lambda _: open(page, 'rb').read(),
}
with zipfile.ZipFile(src) as zi, zipfile.ZipFile(dst, 'w') as zo:
    for info in zi.infolist():
        data = zi.read(info)
        if info.filename in patch: data = patch[info.filename](data)
        out = zipfile.ZipInfo(info.filename, info.date_time)
        out.compress_type, out.external_attr = info.compress_type, info.external_attr
        zo.writestr(out, data)
PY
zipalign -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

# même clé de débogage que Lama Splash (mots de passe publics « android »)
KS="$HERE/../lama-splash/debug.keystore"
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$OUT/signed.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/signed.apk"
cp "$OUT/signed.apk" "$HERE/LamaCourse.apk"
ls -l "$HERE/LamaCourse.apk"
