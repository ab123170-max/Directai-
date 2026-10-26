import type { VercelRequest, VercelResponse } from '@vercel/node';
import { getGeminiClient, GEMINI_MODEL } from '../_lib/gemini';

export default async function handler(_req: VercelRequest, res: VercelResponse) {
  const startedAt = Date.now();
  try {
    const ai = getGeminiClient();
    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: 'Reply with exactly: GEMINI_OK',
    });
    const answer = (response.text || '').trim();
    if (!answer) return res.status(502).json({ ok: false, provider: 'gemini', reason: 'Gemini returned an empty response.' });
    return res.status(200).json({ ok: true, provider: 'gemini', model: GEMINI_MODEL, latencyMs: Date.now() - startedAt, response: answer.slice(0, 100) });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Gemini request failed.';
    const status = message.includes('not configured') ? 503 : 502;
    return res.status(status).json({ ok: false, provider: 'gemini', model: GEMINI_MODEL, reason: message });
  }
}
