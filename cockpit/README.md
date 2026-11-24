# Cockpit : un « CarPlay » pour Android

App plein écran pour un téléphone posé à l'horizontale sur un support de voiture (pensée pour le Nothing Phone, format 20:9).

## Ce que fait la version 0.2

- **Barre latérale** : heure, batterie, raccourcis Waze / Musique / Messages, **bouton Claude** (ouvre l'app Claude), bouton Accueil / Toutes les apps. À gauche ou à droite, au choix.
- **Barre flottante** : quand Waze, la musique, Claude ou une autre app est ouverte depuis Cockpit, la barre reste affichée par-dessus, au même endroit. Le bouton du bas ramène à Cockpit ; un appui long (ou « Quitter » dans la notification) arrête le mode voiture.
- **Tableau de bord**
  - Carte sombre centrée sur ta position (MapLibre + OpenFreeMap, gratuit, sans clé). Un tap ouvre **Waze**.
  - **Lecture en cours** : titre, pochette, progression et commandes de l'app audio active (Spotify, Deezer, YouTube Music, radio…).
  - **Prochain rendez-vous** de l'agenda (24 h).
  - **Météo** du lieu (Open-Meteo, gratuit, sans clé).
- **Grille d'apps** : Waze, Musique, Téléphone, Messages, Claude, Réglages. « Commandes » (voix) et « Véhicule » (OBD‑II) arrivent ensuite.
- **Réglages** : choix de l'app de musique, côté de la barre, état des autorisations, « Quitter le mode voiture ».

## Installer sur le téléphone

1. Récupère `app-release.apk` (envoyé dans la conversation, ou à reconstruire, voir plus bas).
2. Ouvre-le sur le téléphone et autorise l'installation depuis cette source.
3. Au premier lancement, accepte **Position** et **Agenda**.
4. Dans la carte Musique, touche **Autoriser** puis active **Cockpit** dans « Accès aux notifications ». C'est ce qui permet de piloter Spotify & co.
5. Sur la carte, touche **Garder la barre visible dans Waze** et active « Afficher par-dessus les autres applis ».
6. Dans Réglages, choisis ton **app de musique**.

L'APK est signé avec une clé de debug générée sur la machine de build. Si une future version est construite sur une autre machine, Android refusera la mise à jour : désinstalle d'abord l'ancienne.

## Construire

Prérequis : JDK 17+ et le SDK Android (plateforme 35).

```bash
cd cockpit
echo "sdk.dir=/chemin/vers/android-sdk" > local.properties
./gradlew assembleRelease          # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest        # captures des écrans dans app/build/screens/
```

## Organisation du code

| Fichier | Rôle |
| --- | --- |
| `ui/CockpitApp.kt` | Écran racine, permissions, composants communs |
| `ui/Rail.kt` | Barre latérale (partagée avec la barre flottante) |
| `ui/SettingsScreen.kt` | Réglages |
| `overlay/RailOverlayService.kt` | Barre flottante au-dessus des autres apps |
| `data/Prefs.kt` | Choix enregistrés (app de musique, côté de la barre) |
| `ui/Dashboard.kt` | Carte, lecture en cours, agenda, météo |
| `ui/AppGrid.kt` | Grille d'apps |
| `apps/Launchers.kt` | Ouverture des autres apps (Claude, Waze, musique, messages…) |
| `media/NowPlaying.kt` | Suivi et pilotage de l'app audio active |
| `data/Context.kt` | Position, agenda, météo |

## Prochaines étapes

1. Commandes vocales gratuites (musique, appel, guidage) avec la reconnaissance vocale d'Android.
2. Lecture des messages à voix haute et réponse dictée.
3. Ouverture automatique à la connexion Bluetooth de la voiture.
4. Écran Véhicule avec un boîtier OBD‑II Bluetooth.
