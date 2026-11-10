#!/usr/bin/env bash
# Construit Cadran.apk sans Gradle.
# Prérequis (Ubuntu) : apt install aapt apksigner zipalign dalvik-exchange android-sdk-platform-23 et un JDK.
# Polices : FONT_SANS et FONT_MONO pointent vers les fichiers woff2 de @fontsource-variable/geist et geist-mono (npm).
# Signature : KS_PASS (mot de passe de la clé) ; la clé est créée au premier lancement dans KEYSTORE.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="${OUT:-$HERE/build}"
ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
FONT_SANS="${FONT_SANS:?chemin vers geist-latin-wght-normal.woff2}"
FONT_MONO="${FONT_MONO:?chemin vers geist-mono-latin-wght-normal.woff2}"
KS="${KEYSTORE:-$HERE/cadran.keystore}"
KS_PASS="${KS_PASS:?mot de passe de la clé de signature (KS_PASS)}"
export KS_PASS

rm -rf "$OUT" && mkdir -p "$OUT/assets/fonts" "$OUT/classes"
cp "$FONT_SANS" "$OUT/assets/fonts/geist.woff2"
cp "$FONT_MONO" "$OUT/assets/fonts/geist-mono.woff2"

# Page hors ligne : document complet, polices locales
python3 -I - "$HERE/web/cadran.html" "$OUT/assets/index.html" <<'PY'
import re, sys
s = open(sys.argv[1], encoding="utf8").read()
s = re.sub(r'<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>\n?', '', s)
s = s.replace('<meta name="claude:viewport-fit-sides" content="cover">\n', '')
faces = ('@font-face{font-family:"Geist";src:url(fonts/geist.woff2) format("woff2");font-weight:100 900;font-display:swap}\n'
         '@font-face{font-family:"Geist Mono";src:url(fonts/geist-mono.woff2) format("woff2");font-weight:100 900;font-display:swap}\n')
s = s.replace('<style>\n', '<style>\n' + faces, 1)
i = s.index('<div class="app"')
head = ('<!doctype html>\n<html lang="fr">\n<head>\n<meta charset="utf-8">\n'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n')
s = head + s[:i] + '</head>\n<body>\n' + s[i:] + '</body>\n</html>\n'
assert 'googleapis' not in s
open(sys.argv[2], "w", encoding="utf8").write(s)
PY

aapt package -f -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -A "$OUT/assets" -I "$ANDROID_JAR" \
  --min-sdk-version 23 --target-sdk-version 34 --version-code 2 --version-name 1.1 \
  -0 arsc -0 woff2 -F "$OUT/unsigned.apk"
javac -nowarn -source 8 -target 8 -bootclasspath "$ANDROID_JAR" -d "$OUT/classes" "$HERE"/src/app/cadran/*.java 2>&1 | grep -v -i "warning" || true
dalvik-exchange --dex --min-sdk-version=23 --output="$OUT/classes.dex" "$OUT/classes"
(cd "$OUT" && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass:env KS_PASS -keypass:env KS_PASS -alias cadran \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Cadran" 2>&1 | grep -v Picked
fi
apksigner sign --ks "$KS" --ks-pass env:KS_PASS --key-pass env:KS_PASS --out "$HERE/Cadran.apk" "$OUT/aligned.apk"
apksigner verify --verbose "$HERE/Cadran.apk" | grep -E "Verifies|v2|v3"
ls -l "$HERE/Cadran.apk"
