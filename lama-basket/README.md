# Lama Basket

Jeu mobile Android de basket façon fête foraine : on lance le ballon d'un glissement de doigt
vers le haut dans un panier qui se déplace de gauche à droite. La scène est l'aperçu validé, en
illustrations HD générées avec Canva : chapiteau rayé, panier lumineux suspendu à un rail,
écrans LAMA TV avec des lamas qui dansent, rideaux, cornets dorés et ballon posé sur un podium.

## Installer l'APK sur le téléphone

1. Ouvrir la page **Releases** du dépôt sur le téléphone, puis la dernière « Lama Basket (build N) ».
2. Télécharger `lama-basket-N.apk` et l'ouvrir. Autoriser l'installation depuis le navigateur si Android le demande.
3. Les nouvelles versions s'installent par-dessus l'ancienne : la progression est conservée.

Chaque push qui modifie `lama-basket/` relance la compilation (workflow `Lama Basket - APK Android`).

## Contenu du jeu

| Élément | Détail |
| --- | --- |
| Lancer | Glisser vers le haut. La direction vise, la longueur et la vitesse du geste règlent la distance. |
| Panier | Se déplace de gauche à droite, de plus en plus vite ; puis pauses, demi-tours, accélérations et mouvement vertical selon les niveaux. |
| Niveaux | 80 niveaux d'une minute, objectif de 15 à 40 points ; 2 étoiles à 1,5 × l'objectif, 3 étoiles à 2 ×. |
| Points | Panier 2 pts, parfait (sans toucher le cercle ni la planche) 3 pts, ballon doré +5 pts. |
| Combos | Paniers d'affilée : x2 dès le 5e, x3 dès le 9e, x4 dès le 13e. Un raté remet la série à zéro. |
| Ballons | 16 ballons à gagner, un tous les 5 niveaux (plus le ballon classique de départ), à choisir dans le menu Ballons. |
| Décors | Une nouvelle ambiance tous les 10 niveaux sur la scène HD (ciel, éclairage et effets) : Le grand chapiteau, Fête de jour, Coucher de soleil, Nuit & feux d'artifice, Grande roue sous la neige, Fête sur la plage, Fête hantée, Gala des lumières. |
| Animations | Lamas qui dansent sur les écrans, ampoules qui clignotent, panier et chariot qui glissent sur le rail, grande roue qui tourne dans le ciel, feux d'artifice, neige, confettis… |
| Son | Effets et petite musique de manège synthétisés (réglables dans le menu). |

## Organisation

- `web/index.html` : tout le jeu (HTML5 Canvas). Il s'ouvre aussi dans un navigateur pour tester.
- `web/img/` : illustrations HD (décor, panneau, chariot, ballon, lama, masque du ciel) et `assets.json` (repères).
  Si elles manquent, le jeu retombe sur des décors dessinés en code.
- `tools/prep_assets.py` : prépare ces images à partir des PNG exportés de Canva (détourage des fonds verts,
  effacement du chariot peint, masque du ciel, repérage des ampoules). Les repères de `assets.json` sont recopiés
  dans `HD_META` en tête du module HD de `index.html`.
- `android/` : application Android minimale (WebView plein écran) qui embarque `web/`.
- `android/app/lama-basket.keystore` : clé de signature fixe (mot de passe `lamabasket`) qui permet d'installer
  les mises à jour par-dessus. Le dépôt étant public, elle ne sert qu'à cela ; ne pas l'utiliser pour une publication sur le Play Store.

## Réglages

Les constantes en tête du script de `web/index.html` règlent la difficulté (`SPEED_A`, `SPEED_B`, `PAUSE_RATE`,
`FLIP_RATE`, `POWER_K`) et `levelCfg()` donne les objectifs. Dans la console du navigateur,
`__lb.botRun(niveau, écartLatéral, écartProfondeur, cadence)` fait jouer un niveau complet à un robot
pour vérifier l'équilibrage (par exemple `__lb.botRun(40, 0.11, 0.16, 1.15)` pour un joueur moyen).
