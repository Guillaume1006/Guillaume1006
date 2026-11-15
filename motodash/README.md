# MotoDash

Tableau de bord moto pour Android, façon CarPlay, directement sur l'écran du téléphone.

**Ton appli GPS** (Waze, Google Maps, Calimoto…) s'ouvre en grand, et **MotoDash** s'affiche à côté dans un panneau : fond dégradé, cartes en verre dépoli, gros boutons utilisables avec des gants.

![Rendu : GPS à gauche, MotoDash par-dessus à droite](screenshots/rendu-par-dessus.png)

## Deux façons d'afficher le GPS (Réglages)

- **Par-dessus le GPS, comme CarPlay (par défaut).** Le GPS s'ouvre en plein écran et MotoDash s'affiche par-dessus, dans le tiers droit, avec un cadre arrondi autour de la carte. Les touches sur la carte vont au GPS, celles sur le panneau vont à MotoDash. Le ✕ du panneau (ou la notification) le ferme.
  - *Limite* : le GPS croit occuper tout l'écran, donc ce qu'il affiche tout à droite passe sous le panneau. Chez Waze, c'est le bouton « signaler » ; tu peux signaler à la voix (« OK Waze »).
- **Écran partagé Android.** Le GPS et MotoDash sont dans deux fenêtres séparées. Le GPS s'adapte à sa fenêtre, mais c'est à toi de régler la barre du milieu (touche-la deux fois pour échanger les côtés, fais-la glisser pour agrandir le GPS).

## Ce que contient le panneau

- **En haut** : l'heure, la batterie et le bouton micro de l'assistant vocal.
- **La carte Musique** pour l'appli de ton choix (Spotify, Deezer, YouTube Music…) : pochette, titre, artiste, barre de progression et précédent / lecture / suivant. Touche la pochette pour ouvrir l'appli musique.
- **Le dock** :
  - **GPS** rouvre ton appli GPS à côté. Un appui long permet de changer d'appli.
  - **Musique** et **Appels** (contacts favoris en un seul appui).
  - **Applis** : tes raccourcis.
  - **Réglages** : appli GPS, appli musique, ouverture du GPS au démarrage.

L'écran reste allumé et MotoDash s'affiche par-dessus l'écran verrouillé.

## Installer

1. Copie `MotoDash.apk` sur le téléphone et ouvre-le (autorise « Installer des applis inconnues »).
2. Choisis ton **appli GPS** et ton **appli musique**. MotoDash propose d'abord celles qu'il a détectées.
3. Touche **Démarrer**, puis autorise MotoDash à **s'afficher par-dessus les autres applis**.
4. Pour voir le titre en cours, MotoDash a besoin de « l'accès aux notifications ». Android 13+ le bloque pour les applis installées hors du Play Store :
   - **Réglages → Applis → MotoDash → ⋮ (en haut à droite) → Autoriser les paramètres restreints** ;
   - puis, dans MotoDash, touche « Autoriser l'affichage du titre » et active MotoDash.

## Mode écran partagé

Sur Android 12L et plus, l'écran se partage tout seul quand MotoDash ouvre le GPS.

- **Pour agrandir le GPS**, fais glisser la barre du milieu vers MotoDash (2/3 pour le GPS, 1/3 pour MotoDash).
- **Si le GPS est du mauvais côté**, touche deux fois la barre du milieu pour échanger les côtés.
- **Si l'écran ne se partage pas tout seul** : ouvre MotoDash, puis **Récents**, touche l'icône MotoDash, puis **Écran partagé**, et choisis ton GPS.

## Compiler

```bash
./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
```

## Rendus

| Écran partagé 50 / 50 (capture réelle) | Premier lancement |
|---|---|
| ![](screenshots/rendu-moitie.png) | ![](screenshots/rendu-premier-lancement.png) |
| **Choix de l'appli GPS** | **Choix de l'appli musique** |
| ![](screenshots/rendu-choix-gps.png) | ![](screenshots/rendu-choix-musique.png) |

*Les rendus utilisent une fausse appli GPS et un faux lecteur créés pour les captures. Sur ton téléphone, ce sont tes vraies applis (Waze, Spotify…).*
