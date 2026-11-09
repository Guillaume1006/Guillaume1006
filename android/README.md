# Ma Maison — application Android

Application Android qui ouvre « Ma Maison » (le dossier `../domotique`) en plein écran, comme une vraie application : icône sur l'écran d'accueil, pas de barre d'adresse, bouton Retour qui ferme les fenêtres puis revient à la page précédente.

L'application web est **embarquée dans l'APK** : pas besoin d'héberger les fichiers. L'appli se connecte directement à ton Home Assistant, sur le réseau local (`http://192.168.x.x:8123`) comme à distance (`https://…ui.nabu.casa`).

## Installer l'APK sur le téléphone

1. Copie `MaMaison-1.0.apk` sur le téléphone (ou ouvre-le depuis le téléchargement).
2. Touche le fichier. Android demande d'autoriser l'installation d'applications inconnues pour l'application qui l'ouvre (Fichiers, Chrome…) : accepte.
3. Ouvre **Ma Maison** → **Réglages** → **Home Assistant** : adresse et jeton d'accès longue durée.

Conseils :

- Sur le réseau local, utilise de préférence l'**adresse IP** de Home Assistant (`http://192.168.1.20:8123`) : les noms en `.local` ne sont pas reconnus par tous les téléphones Android.
- Hors de chez toi, il faut une adresse joignable depuis Internet (Nabu Casa, DuckDNS…).
- Le jeton est stocké uniquement dans l'application, sur le téléphone.

## Compiler

Prérequis : JDK 17 ou plus récent, SDK Android (plateforme 35). Android Studio ouvre aussi directement ce dossier.

```bash
cd android
echo "sdk.dir=/chemin/vers/android-sdk" > local.properties
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Chaque compilation reprend la dernière version du dossier `domotique/` : il suffit de recompiler pour mettre l'application à jour.

### Signature

Android n'accepte une mise à jour que si elle est signée avec la **même clé** que la version installée. Pour signer avec ta propre clé, crée un fichier `keystore.properties` (jamais versionné) :

```properties
storeFile=/chemin/vers/ma-cle.jks
storePassword=…
keyAlias=maison
keyPassword=…
```

Sans ce fichier, la version release est signée avec la clé de débogage de la machine. Si la clé change, désinstalle l'ancienne version avant d'installer la nouvelle (il faudra ressaisir l'adresse et le jeton dans les réglages).

## Fonctionnement

- `MainActivity` affiche une WebView qui sert les fichiers de l'appli depuis les assets, à une adresse fictive (`http://appassets.androidplatform.net/`). Rien ne transite sur le réseau pour ces fichiers.
- Le trafic `http://` / `ws://` est autorisé pour joindre Home Assistant en local. Les certificats HTTPS restent vérifiés normalement.
- Android 8.0 (API 26) minimum.
