const Alexa = require('ask-sdk-core');
const { askClaude } = require('./claude');

const MAX_TURNS = 10; // messages conservés en session (5 échanges)

const escapeSsml = (s) =>
  s.replace(/&/g, ' et ').replace(/</g, ' ').replace(/>/g, ' ').replace(/"/g, '');

const LaunchRequestHandler = {
  canHandle: (h) => Alexa.getRequestType(h.requestEnvelope) === 'LaunchRequest',
  handle: (h) =>
    h.responseBuilder
      .speak("Bonjour, je suis connecté à Claude. Que puis-je faire pour vous ?")
      .reprompt('Posez-moi une question.')
      .getResponse(),
};

const ChatIntentHandler = {
  canHandle: (h) =>
    Alexa.getRequestType(h.requestEnvelope) === 'IntentRequest' &&
    Alexa.getIntentName(h.requestEnvelope) === 'ChatIntent',
  async handle(h) {
    const query = Alexa.getSlotValue(h.requestEnvelope, 'query');
    const attrs = h.attributesManager.getSessionAttributes();
    const history = (attrs.history || []).concat({ role: 'user', content: query });

    let answer;
    try {
      answer = await askClaude(history);
    } catch (err) {
      console.error(err);
      return h.responseBuilder
        .speak("Désolé, je n'ai pas réussi à joindre Claude. Réessayez dans un instant.")
        .reprompt('Voulez-vous réessayer ?')
        .getResponse();
    }

    attrs.history = history.concat({ role: 'assistant', content: answer }).slice(-MAX_TURNS);
    h.attributesManager.setSessionAttributes(attrs);

    return h.responseBuilder
      .speak(escapeSsml(answer))
      .reprompt('Autre chose ?')
      .getResponse();
  },
};

const HelpHandler = {
  canHandle: (h) =>
    Alexa.getRequestType(h.requestEnvelope) === 'IntentRequest' &&
    Alexa.getIntentName(h.requestEnvelope) === 'AMAZON.HelpIntent',
  handle: (h) =>
    h.responseBuilder
      .speak('Vous pouvez me poser n\'importe quelle question, par exemple : demande quelle est la capitale du Japon.')
      .reprompt('Quelle est votre question ?')
      .getResponse(),
};

const StopHandler = {
  canHandle: (h) =>
    Alexa.getRequestType(h.requestEnvelope) === 'IntentRequest' &&
    ['AMAZON.CancelIntent', 'AMAZON.StopIntent'].includes(Alexa.getIntentName(h.requestEnvelope)),
  handle: (h) => h.responseBuilder.speak('À bientôt !').getResponse(),
};

const FallbackHandler = {
  canHandle: (h) =>
    Alexa.getRequestType(h.requestEnvelope) === 'IntentRequest' &&
    Alexa.getIntentName(h.requestEnvelope) === 'AMAZON.FallbackIntent',
  handle: (h) =>
    h.responseBuilder
      .speak('Je n\'ai pas compris. Essayez : demande, suivi de votre question.')
      .reprompt('Quelle est votre question ?')
      .getResponse(),
};

const SessionEndedHandler = {
  canHandle: (h) => Alexa.getRequestType(h.requestEnvelope) === 'SessionEndedRequest',
  handle: (h) => h.responseBuilder.getResponse(),
};

const ErrorHandler = {
  canHandle: () => true,
  handle(h, err) {
    console.error(err);
    return h.responseBuilder.speak('Une erreur est survenue.').reprompt('Pouvez-vous répéter ?').getResponse();
  },
};

exports.handler = Alexa.SkillBuilders.custom()
  .addRequestHandlers(
    LaunchRequestHandler,
    ChatIntentHandler,
    HelpHandler,
    StopHandler,
    FallbackHandler,
    SessionEndedHandler
  )
  .addErrorHandlers(ErrorHandler)
  .lambda();
