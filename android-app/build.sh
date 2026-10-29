#!/usr/bin/env bash
# Construit Coupe-Son.apk sans Gradle.
# Prérequis (Ubuntu) : apt install aapt apksigner zipalign dalvik-exchange android-sdk-platform-23 openjdk-17-jdk (ou 21)
# Les polices Geist viennent de npm : @fontsource-variable/geist et @fontsource-variable/geist-mono.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
OUT="${OUT:-$HERE/build}"
ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
LAME="${LAME:?chemin vers lame.min.js (npm pack lamejs@1.2.1)}"
FONT_SANS="${FONT_SANS:?chemin vers geist-latin-wght-normal.woff2}"
FONT_MONO="${FONT_MONO:?chemin vers geist-mono-latin-wght-normal.woff2}"

rm -rf "$OUT" && mkdir -p "$OUT/assets/fonts" "$OUT/classes"
cp "$LAME" "$OUT/assets/lame.min.js"
cp "$FONT_SANS" "$OUT/assets/fonts/geist.woff2"
cp "$FONT_MONO" "$OUT/assets/fonts/geist-mono.woff2"

# Page hors ligne : encodeur et polices locaux
python3 -I - "$ROOT/coupe-son/index.html" "$OUT/assets/index.html" <<'PY'
import re, sys
s = open(sys.argv[1], encoding="utf8").read()
s = re.sub(r'<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>\n?', '', s)
s = s.replace('https://cdnjs.cloudflare.com/ajax/libs/lamejs/1.2.1/lame.min.js', 'lame.min.js')
faces = ('@font-face{font-family:"Geist";src:url(fonts/geist.woff2) format("woff2");font-weight:100 900;font-display:swap}\n'
         '@font-face{font-family:"Geist Mono";src:url(fonts/geist-mono.woff2) format("woff2");font-weight:100 900;font-display:swap}\n')
s = s.replace('<style>\n', '<style>\n' + faces, 1)
assert 'googleapis' not in s and 'cdnjs' not in s
open(sys.argv[2], "w", encoding="utf8").write(s)
PY

aapt package -f -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -A "$OUT/assets" -I "$ANDROID_JAR" \
  --min-sdk-version 29 --target-sdk-version 34 --version-code 1 --version-name 1.0 \
  -0 arsc -0 woff2 -F "$OUT/unsigned.apk"
javac -nowarn -source 8 -target 8 -bootclasspath "$ANDROID_JAR" -d "$OUT/classes" "$HERE"/src/app/coupeson/*.java 2>&1 | grep -v "warning" || true
dalvik-exchange --dex --min-sdk-version=26 --output="$OUT/classes.dex" "$OUT/classes"
(cd "$OUT" && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KS="${KEYSTORE:-$HERE/coupeson.keystore}"
KS_PASS="${KS_PASS:?mot de passe de la clé de signature (KS_PASS)}"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass "$KS_PASS" -keypass "$KS_PASS" -alias coupeson \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Coupe-Son" 2>&1 | grep -v Picked
fi
apksigner sign --ks "$KS" --ks-pass env:KS_PASS --key-pass env:KS_PASS --out "$HERE/Coupe-Son.apk" "$OUT/aligned.apk"
apksigner verify --verbose "$HERE/Coupe-Son.apk" | head -6
ls -l "$HERE/Coupe-Son.apk"
