import type { VercelRequest, VercelResponse } from '@vercel/node';
import { getGeminiClient, GEMINI_MODEL, Type } from '../_lib/gemini';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).json({ success: false, fallback: false, error: 'Method not allowed.' });
  try {
    const { imageBase64, mode, currentShot } = req.body || {};
    const ai = getGeminiClient();
    const parts: any[] = [];
    if (typeof imageBase64 === 'string' && imageBase64.length > 0) {
      const match = imageBase64.match(/^data:(image\/[\w.+-]+);base64,(.*)$/s);
      parts.push({ inlineData: { mimeType: match?.[1] || 'image/jpeg', data: match?.[2] || imageBase64 } });
    }
    parts.push({ text: `You are an expert AI camera director. Analyze the scene for shooting mode "${String(mode || 'general').slice(0, 80)}" and current shot "${String(currentShot || 'Establishing Shot').slice(0, 120)}". Give specific, safe, actionable guidance for a phone camera. Return subject, lighting, composition, stability, direction, movementSuggestion, and duration (integer seconds).` });
    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: { parts },
      config: {
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            subject: { type: Type.STRING }, lighting: { type: Type.STRING },
            composition: { type: Type.STRING }, stability: { type: Type.STRING },
            direction: { type: Type.STRING }, movementSuggestion: { type: Type.STRING },
            duration: { type: Type.INTEGER },
          },
          required: ['subject', 'lighting', 'composition', 'stability', 'direction', 'movementSuggestion', 'duration'],
        },
      },
    });
    const result = JSON.parse(response.text || '{}');
    if (!result.subject || !result.direction) throw new Error('Gemini returned an incomplete scene analysis.');
    return res.status(200).json({ success: true, fallback: false, model: GEMINI_MODEL, ...result });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Gemini scene analysis failed.';
    const status = message.includes('not configured') ? 503 : 502;
    return res.status(status).json({ success: false, fallback: false, error: message });
  }
}
