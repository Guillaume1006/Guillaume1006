#!/usr/bin/env bash
# Construit A3Cockpit.apk sans Gradle, avec les outils Android d'Ubuntu/Debian :
#   sudo apt install aapt android-sdk-platform-23 dalvik-exchange zipalign apksigner openjdk-17-jdk
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/classes"

echo "▸ Ressources"
aapt package -f -m -J "$OUT/gen" -M AndroidManifest.xml -S res -I "$SDK" -F "$OUT/res.apk"

echo "▸ Compilation Java"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -bootclasspath "$SDK" \
      -d "$OUT/classes" $(find src "$OUT/gen" -name '*.java')

echo "▸ Dex"
dalvik-exchange --dex --min-sdk-version=23 --output="$OUT/classes.dex" "$OUT/classes"

echo "▸ Assemblage"
cp "$OUT/res.apk" "$OUT/unsigned.apk"
(cd "$OUT" && zip -q unsigned.apk classes.dex)
zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KS=signing/a3cockpit.jks
if [ ! -f "$KS" ]; then
  mkdir -p signing
  keytool -genkeypair -keystore "$KS" -storepass a3cockpit -keypass a3cockpit -alias a3cockpit \
          -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=A3 Cockpit" >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-pass pass:a3cockpit --key-pass pass:a3cockpit --ks-key-alias a3cockpit \
          --out A3Cockpit.apk "$OUT/aligned.apk"
apksigner verify A3Cockpit.apk
echo "✔ A3Cockpit.apk ($(du -h A3Cockpit.apk | cut -f1))"
