# Alexa × Claude

Skill Alexa en français : « Alexa, ouvre mon assistant », puis pose tes questions, Claude répond via l'enceinte.
La conversation est mémorisée pendant la session (5 échanges).

## Structure

- `lambda/` : code Node.js (AWS Lambda) – `index.js` (handlers Alexa), `claude.js` (appel API Anthropic)
- `skill-package/` : manifeste et modèle d'interaction `fr-FR` (nom d'invocation : « mon assistant »)

## Déploiement

1. Crée une clé API sur https://console.anthropic.com.
2. Console Alexa Developer → *Create skill* → *Custom* → *Alexa-hosted (Node.js)* ou *Provision your own*.
3. Colle `skill-package/interactionModels/custom/fr-FR.json` dans l'onglet *Build → JSON Editor*, puis *Build*.
4. Dans le code, copie `lambda/` (`npm install` si Lambda perso).
5. Ajoute la variable d'environnement `ANTHROPIC_API_KEY` (et optionnellement `CLAUDE_MODEL`, défaut `claude-haiku-5-5`).
6. Onglet *Test* → active « Development », dis : « ouvre mon assistant », puis « demande quelle est la capitale du Japon ».

Ou avec la ASK CLI : `ask deploy` depuis ce dossier (ajoute le Lambda dans `ask-resources.json`).

## Limites

- Alexa coupe la réponse après ~8 s : le modèle rapide (Haiku) et 300 tokens max sont voulus.
- Les phrases doivent commencer par un mot déclencheur (« demande… », « dis-moi… », « explique-moi… ») : contrainte du type `AMAZON.SearchQuery`.
- La clé API ne doit jamais être commitée.
