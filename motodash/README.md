# MotoDash

Tableau de bord moto pour Android, façon CarPlay, directement sur l'écran du téléphone.

- **Une moitié : la carte GPS** (OpenStreetMap, sans compte ni clé). Elle suit ta position, avec la vitesse en surimpression, un mode nuit, la carte orientée vers ton cap, et un gros bouton **Navigation** qui ouvre ton appli GPS préférée (Waze, Google Maps, Calimoto…). Un appui long sur ce bouton permet de changer d'appli.
- **L'autre moitié : des onglets à gros boutons** (utilisables avec des gants)
  - **Compteur** : vitesse, vitesse max, distance du trajet, durée, altitude
  - **Musique** : lecture/pause, piste suivante/précédente, volume, titre et pochette du morceau en cours
  - **Appels** : contacts favoris en un seul appui, plus le clavier
  - **Applis** : tes applis au choix, en raccourci

Il s'adapte au paysage (carte à gauche, panneau à droite) comme au portrait (carte en haut). Le bouton ⇄ inverse les deux moitiés. L'écran reste allumé et l'appli s'affiche par-dessus l'écran verrouillé.

## Installer

1. Copie `MotoDash.apk` sur le téléphone et ouvre-le (autorise « Installer des applis inconnues »).
2. Au premier lancement, accepte la localisation.
3. Onglet Musique : touche le bandeau orange et active **MotoDash** dans « Accès aux notifications ». C'est nécessaire pour afficher le titre en cours. Les boutons lecture et suivant fonctionnent même sans cette autorisation.

## Afficher une autre appli dans la moitié de l'écran

Android ne permet pas à une appli d'en afficher une autre *à l'intérieur* de sa propre fenêtre. On passe donc par l'écran partagé d'Android :

1. Ouvre MotoDash, puis le menu **Récents** (le carré, ou un glissement vers le haut en le maintenant).
2. Touche l'icône de MotoDash, puis **Écran partagé**.
3. Choisis l'autre appli.

Une fois MotoDash en écran partagé, les applis lancées depuis l'onglet Applis ou le bouton Navigation s'ouvrent automatiquement dans l'autre moitié.

## Compiler

```bash
./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
```
