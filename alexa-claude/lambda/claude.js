const API_URL = 'https://api.anthropic.com/v1/messages';
const MODEL = process.env.CLAUDE_MODEL || 'claude-haiku-5-5';
const TIMEOUT_MS = 6500; // Alexa coupe la réponse à ~8 s

const SYSTEM_PROMPT =
  "Tu es un assistant vocal en français, utilisé via une enceinte Alexa. " +
  "Réponds en une à trois phrases courtes, en langage parlé : pas de listes, " +
  "pas de markdown, pas d'émojis, pas d'URL. Si la question est ambiguë, pose une seule question de précision.";

async function askClaude(history) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(API_URL, {
      method: 'POST',
      signal: ctrl.signal,
      headers: {
        'content-type': 'application/json',
        'x-api-key': process.env.ANTHROPIC_API_KEY,
        'anthropic-version': '2023-06-01',
      },
      body: JSON.stringify({
        model: MODEL,
        max_tokens: 300,
        system: SYSTEM_PROMPT,
        messages: history,
      }),
    });
    if (!res.ok) throw new Error(`Claude API ${res.status}: ${await res.text()}`);
    const data = await res.json();
    return data.content.filter((b) => b.type === 'text').map((b) => b.text).join(' ').trim();
  } finally {
    clearTimeout(timer);
  }
}

module.exports = { askClaude };
