#!/usr/bin/env bash
# Construit iOS-Icons.apk (nécessite : aapt, dalvik-exchange, apksigner, zipalign, openjdk, python3-pillow)
set -euo pipefail
cd "$(dirname "$0")"
ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
B=$(mktemp -d)
python3 make_icons.py "$B/pkg"
cp AndroidManifest.xml "$B/pkg/"
aapt package -f -M "$B/pkg/AndroidManifest.xml" -S "$B/pkg/res" -A "$B/pkg/assets" \
     -I "$ANDROID_JAR" -F "$B/unsigned.apk"
mkdir -p "$B/classes"
javac --release 8 -cp "$ANDROID_JAR" -d "$B/classes" src/com/guillaume/iosicons/MainActivity.java 2>&1 | grep -v warning || true
dalvik-exchange --dex --output="$B/classes.dex" "$B/classes"
(cd "$B" && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -f 4 "$B/unsigned.apk" "$B/aligned.apk"
KS="${KEYSTORE:-$HOME/.ios-icons.jks}"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias key \
     -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=iOS Icons" >/dev/null 2>&1
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out ../iOS-Icons.apk "$B/aligned.apk"
apksigner verify -v ../iOS-Icons.apk | head -5
rm -rf "$B"
