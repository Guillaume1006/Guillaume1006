# Cockpit : un « CarPlay » pour Android

App plein écran pour un téléphone posé à l'horizontale sur un support de voiture (pensée pour le Nothing Phone, format 20:9).

## Ce que fait la version 0.4

- **Barre latérale** collée au bord de l'écran (par-dessus la zone de la caméra) : heure, batterie, Waze, Musique, Messages, **Claude**, Accueil / Toutes les apps. À gauche ou à droite, au choix.
- **Tout s'ouvre dans Cockpit** : lecteur musique, messages et téléphone sont des écrans de Cockpit, la barre ne bouge pas.
- **Waze et Claude** (qui ne peuvent pas s'afficher à l'intérieur d'une autre app) s'ouvrent sans animation, avec la barre déjà en place par-dessus, au même endroit, et le téléphone gardé à l'horizontale. La barre disparaît quand tu pars dans les menus du téléphone.
- **Lecteur musique** : l'app choisie (Spotify, Deezer…) se pilote depuis Cockpit : titre, pochette, progression, lecture/pause, précédent/suivant. Playlists affichées quand l'app les partage (Spotify les réserve à Android Auto).
- **Messages** : SMS, WhatsApp, Messenger… reçus pendant la conduite, lus à voix haute, réponse dictée.
- **Téléphone** : contacts favoris en grosses tuiles, un tap appelle.
- **Tableau de bord** : carte centrée sur ta position (un tap ouvre Waze), musique, prochain rendez-vous, météo.
- **Réglages** : app de musique, côté de la barre, autorisations, « Quitter le mode voiture ».

## Installer sur le téléphone

1. Récupère `app-release.apk` (envoyé dans la conversation, ou à reconstruire, voir plus bas).
2. Ouvre-le sur le téléphone et autorise l'installation depuis cette source.
3. Au premier lancement, accepte **Position** et **Agenda**.
4. Dans la carte Musique, touche **Autoriser** puis active **Cockpit** dans « Accès aux notifications ». C'est ce qui permet de piloter Spotify & co.
5. Sur la carte, touche **Garder la barre visible dans Waze** et active « Afficher par-dessus les autres applis ».
6. Dans Réglages, touche **Disparaître dans les menus du téléphone** et active Cockpit dans « Accès aux données d'utilisation ».
7. Touche **Musique** et choisis ton app de musique.

L'APK est signé avec une clé de debug générée sur la machine de build. Si une future version est construite sur une autre machine, Android refusera la mise à jour : désinstalle d'abord l'ancienne.

## Construire

Prérequis : JDK 17+ et le SDK Android (plateforme 35).

```bash
cd cockpit
echo "sdk.dir=/chemin/vers/android-sdk" > local.properties
./gradlew assembleRelease          # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest        # 16 tests : chaque bouton, lecteur, messages, barre flottante
./gradlew recordRoborazziDebug     # captures des écrans dans app/build/screens/
```

## Organisation du code

| Fichier | Rôle |
| --- | --- |
| `ui/CockpitApp.kt` | Écran racine, permissions, composants communs |
| `ui/Rail.kt` | Barre latérale (partagée avec la barre flottante) |
| `ui/MusicScreen.kt` | Lecteur musique intégré |
| `ui/MessagesScreen.kt` | Messages lus et réponse dictée |
| `ui/PhoneScreen.kt` | Favoris et appels |
| `ui/SettingsScreen.kt` | Réglages |
| `overlay/RailOverlayService.kt` | Barre flottante au-dessus des autres apps |
| `data/Prefs.kt` | Choix enregistrés (app de musique, côté de la barre) |
| `ui/Dashboard.kt` | Carte, lecture en cours, agenda, météo |
| `ui/AppGrid.kt` | Grille d'apps |
| `apps/Launchers.kt` | Ouverture des autres apps (Claude, Waze, musique, messages…) |
| `media/NowPlaying.kt` | Suivi et pilotage de l'app audio choisie |
| `media/Library.kt` | Playlists de l'app de musique (quand elle les partage) |
| `media/Messages.kt` | Messages reçus et réponses |
| `data/Context.kt` | Position, agenda, météo |

## Prochaines étapes

1. Commandes vocales gratuites (musique, appel, guidage) avec la reconnaissance vocale d'Android.
3. Ouverture automatique à la connexion Bluetooth de la voiture.
4. Écran Véhicule avec un boîtier OBD‑II Bluetooth.
