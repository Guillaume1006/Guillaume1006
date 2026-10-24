# Jarvis - robot assistant vocal pour vieux téléphone Android

Une seule page web (`index.html`) : yeux animés, bouche qui bouge, écoute vocale, réponses de Claude, météo (Open-Meteo, sans clé) et domotique via Home Assistant.

## Installation sur le téléphone

Le micro n'est autorisé par Chrome que sur une page **HTTPS** (ou `localhost`). Deux solutions :

1. **GitHub Pages (recommandé)** : active Pages sur ce dépôt (dossier `jarvis`), ouvre l'URL `https://...github.io/...` dans Chrome sur le téléphone, puis menu ⋮ → *Ajouter à l'écran d'accueil* (mode plein écran).
2. **En local** : sur un PC, `cd jarvis && python3 -m http.server 8000`, puis sur le téléphone ouvre `http://IP_DU_PC:8000` et, dans `chrome://flags`, active *Insecure origins treated as secure* en y ajoutant cette adresse.

## Réglages (⚙ en haut à droite)

- **Clé API Anthropic** : console.anthropic.com. Stockée seulement dans le téléphone.
- **Home Assistant** : URL + jeton longue durée. Depuis une page HTTPS, l'URL de HA doit aussi être en HTTPS (Nabu Casa, DuckDNS...). Sans HA, la météo, les minuteurs et les questions fonctionnent quand même.
- **Mot de réveil** : « jarvis » par défaut. Tape sur l'écran pour forcer l'écoute.

## Limites connues

- L'écoute continue exige que Chrome reste ouvert, écran allumé (la page demande de garder l'écran actif). Pour une écoute en arrière-plan, il faudra une appli Android native.
- La clé API est dans le navigateur : à réserver à un usage perso.

## Nothing Phone (4a)

- Ouvre l'adresse dans Chrome, puis menu ⋮ → *Installer l'application* : Jarvis s'ouvre en plein écran et en paysage, sans barre d'adresse.
- Réglages → Batterie → Jarvis/Chrome : choisis *Sans restriction* pour éviter que Nothing OS endorme l'appli.
- Branche le téléphone sur le secteur dans sa coque : l'écran reste allumé en permanence.
- Autorise le micro pour Chrome quand il le demande.
