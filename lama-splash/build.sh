#!/usr/bin/env bash
# Reconstruit LamaSplash.apk en remplaçant le jeu (assets/public/index.html) dans un APK Capacitor existant.
# Prérequis (Ubuntu) : apt install apksigner zipalign
# Usage : ./build.sh [apk_de_base]   (par défaut : LamaSplash.apk lui-même)
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
BASE="${1:-$HERE/LamaSplash.apk}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

# Recopie chaque entrée à l'identique (ordre, compression), sauf le jeu qui est remplacé
python3 -I - "$BASE" "$HERE/index.html" "$OUT/unsigned.apk" <<'PY'
import sys, zipfile
src, page, dst = sys.argv[1:]
html = open(page, 'rb').read()
with zipfile.ZipFile(src) as zi, zipfile.ZipFile(dst, 'w') as zo:
    for info in zi.infolist():
        data = html if info.filename == 'assets/public/index.html' else zi.read(info)
        out = zipfile.ZipInfo(info.filename, info.date_time)
        out.compress_type, out.external_attr = info.compress_type, info.external_attr
        zo.writestr(out, data)
PY
zipalign -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

# Clé de débogage Android (mots de passe publics « android »), conservée pour que les mises à jour s'installent par-dessus
KS="$HERE/debug.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>&1 | grep -v Picked || true
fi
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$OUT/signed.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/signed.apk"
cp "$OUT/signed.apk" "$HERE/LamaSplash.apk"
ls -l "$HERE/LamaSplash.apk"
