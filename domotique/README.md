# Ma Maison — application domotique

Tableau de bord pour piloter la maison depuis le téléphone, la tablette ou l’ordinateur :

- **Accueil** : températures intérieure/extérieure, consommation électrique, lumières allumées (bouton « tout éteindre »), état de la serrure, points à vérifier (fenêtre ouverte, fuite, porte déverrouillée), scènes rapides et favoris.
- **Pièces** : chaque pièce avec ses lumières, prises, volets, thermostats, serrures et capteurs.
- **Détail d’un appareil** (bouton ⋯ ou appui long) : luminosité, blanc chaud/froid, couleur, position des volets, consigne et mode du chauffage, verrouillage, ajout aux favoris ★.
- **Énergie** : graphiques de consommation (kWh par heure, sur 7 jours ou 30 jours), courbe de puissance sur 24 h, répartition par appareil, coût estimé et comparaison avec la période précédente.
- **Scènes** et **scripts** en un geste.
- **Automatismes** : activer, désactiver ou lancer une automatisation.
- **Journal** des derniers changements.
- Thème clair/sombre, installable comme une application (PWA), aucune dépendance, aucun serveur à ajouter.

Deux modes :

1. **Maison de démo** (par défaut) : une maison simulée (6 pièces, 34 appareils, scènes, automatismes) pour tout essayer tout de suite.
2. **Home Assistant** : l’application se connecte à ton serveur Home Assistant par son API WebSocket officielle et affiche tes vrais appareils, rangés selon les pièces définies dans HA, en temps réel.

## Lancer l’application

Sur un ordinateur, depuis ce dossier :

```bash
cd domotique
python3 -m http.server 8000
```

Puis ouvre <http://localhost:8000>. Depuis le téléphone, sur le même Wi-Fi : `http://IP_DU_PC:8000`.

## Application Android (APK)

Le dossier [`android/`](../android) contient une application Android qui embarque cette interface : installe l’APK sur le téléphone et l’appli s’ouvre en plein écran, sans navigateur. Voir [`android/README.md`](../android/README.md).

## Brancher Home Assistant

1. Dans Home Assistant : clique sur ton **profil** (en bas à gauche) → onglet **Sécurité** → **Jetons d’accès longue durée** → **Créer un jeton**. Copie-le.
2. Dans l’application : **Réglages** → **Home Assistant** → adresse (ex. `http://homeassistant.local:8123`) et jeton → **Enregistrer et se connecter**.

Les pièces viennent des « zones » (areas) de Home Assistant. Il faut un compte **administrateur** pour les lire ; sinon tout s’affiche quand même dans « Autres ».

### Recommandé : héberger l’application dans Home Assistant

C’est le plus simple et ça évite les problèmes HTTP/HTTPS :

1. Copie le dossier `domotique` dans `/config/www/` de Home Assistant (avec l’add-on *File editor*, *Samba* ou *Studio Code Server*).
2. Ouvre `http://homeassistant.local:8123/local/domotique/index.html`.
3. L’adresse de HA est alors pré-remplie : il ne reste que le jeton à coller.
4. Sur le téléphone, menu ⋮ de Chrome → **Ajouter à l’écran d’accueil** : l’application s’ouvre en plein écran.

Tu peux aussi l’ajouter dans la barre latérale de HA : **Paramètres → Tableaux de bord → Ajouter un tableau de bord → Page web**, avec l’adresse `/local/domotique/index.html`.

### Graphiques de consommation

La page **Énergie** lit les statistiques de Home Assistant, celles de son tableau de bord **Énergie** :

- le **compteur** (ex. Linky via Téléinfo, ou une pince ampèremétrique), avec heures pleines et heures creuses additionnées ;
- les **appareils suivis** (prises connectées qui mesurent la consommation), pour la répartition par appareil ;
- un **capteur de puissance** (W ou kW) pour la courbe sur 24 h.

Configure-le dans HA : **Paramètres → Tableaux de bord → Énergie**. Sans ce tableau de bord, l’application cherche toute seule un capteur en kWh. Tu peux aussi choisir les capteurs et le **prix du kWh** dans **Réglages → Énergie**. Le coût est une estimation : prix unique, sans abonnement ni taxes fixes. La production solaire n’est pas déduite.

Les statistiques de HA sont calculées chaque heure : l’heure en cours n’apparaît qu’une fois terminée.



Un navigateur interdit à une page **HTTPS** de se connecter à une adresse **HTTP**. Si l’application est publiée en HTTPS (GitHub Pages par exemple), Home Assistant doit aussi être en HTTPS (Nabu Casa, DuckDNS + Let’s Encrypt…). L’écran Réglages te prévient si ce n’est pas le cas.

## Ce qui est pris en charge

| Type Home Assistant | Ce que fait l’application |
| --- | --- |
| `light` | allumer/éteindre, luminosité, température de blanc, couleur |
| `switch`, `input_boolean`, `fan` | allumer/éteindre |
| `cover` | ouvrir, fermer, stop, position |
| `climate` | consigne ± , mode (arrêt, chauffage, auto…) |
| `lock` | verrouiller/déverrouiller |
| `sensor` | température, humidité, puissance, énergie, CO₂, luminosité, batterie, pression |
| `binary_sensor` | portes, fenêtres, mouvement, fuite, fumée… (alerte si ouvert/détecté) |
| `scene`, `script` | lancer |
| `automation` | activer/désactiver, exécuter |

Les entités techniques (diagnostic, configuration, masquées dans HA) ne sont pas affichées.

## Sécurité

- Le jeton est enregistré **uniquement dans le navigateur** de l’appareil (localStorage). Il donne accès à toute ta maison : n’utilise l’application que sur tes appareils, et révoque le jeton dans HA si un appareil est perdu.
- Rien ne passe par un service tiers : l’application parle directement à ton Home Assistant.

## Organisation du code

```
domotique/
├── index.html            page unique
├── style.css             thème clair/sombre, mobile d’abord
├── manifest.webmanifest  installation comme application
├── sw.js                 fonctionnement hors ligne de l’interface
├── icon.svg
└── js/
    ├── icons.js          icônes SVG
    ├── format.js         libellés, états, filtres
    ├── demo.js           maison simulée (mêmes services que HA)
    ├── ha.js             connexion WebSocket à Home Assistant (reconnexion automatique)
    ├── energy.js         page Énergie : statistiques, graphiques SVG
    └── app.js            interface, navigation, actions
```

Pas d’étape de compilation : on modifie les fichiers et on recharge la page.
