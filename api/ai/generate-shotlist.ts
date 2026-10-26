import type { VercelRequest, VercelResponse } from '@vercel/node';
import { getGeminiClient, GEMINI_MODEL, Type } from '../_lib/gemini';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).json({ success: false, fallback: false, error: 'Method not allowed.' });
  try {
    const { title, category, description } = req.body || {};
    const ai = getGeminiClient();
    const prompt = `Generate a professional cinematic shot list for a video project titled "${String(title || 'Untitled Project').slice(0, 160)}", category "${String(category || 'general').slice(0, 80)}", description "${String(description || 'Professional video').slice(0, 1800)}".
Return 8 to 12 distinct, practical shots for a solo phone creator. Each shot needs shotNumber (integer starting at 1), name, type (establishing, wide, medium, close-up, extreme-close-up, overhead, low-angle, tracking, reveal, detail, b-roll, hero, reaction, ending), purpose, angle (Eye-Level, Low-Angle, High-Angle, Overhead, Dutch Tilt), movement (static, pan-left, pan-right, tilt-up, tilt-down, push-in, pull-back, orbit, track, reveal), duration (integer seconds), and subject.`;
    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.ARRAY,
          items: {
            type: Type.OBJECT,
            properties: {
              shotNumber: { type: Type.INTEGER }, name: { type: Type.STRING },
              type: { type: Type.STRING }, purpose: { type: Type.STRING },
              angle: { type: Type.STRING }, movement: { type: Type.STRING },
              duration: { type: Type.INTEGER }, subject: { type: Type.STRING },
            },
            required: ['shotNumber', 'name', 'type', 'purpose', 'angle', 'movement', 'duration', 'subject'],
          },
        },
      },
    });
    const shots = JSON.parse(response.text || '[]');
    if (!Array.isArray(shots) || shots.length < 1) throw new Error('Gemini returned no shots. Please try again.');
    return res.status(200).json({ success: true, fallback: false, model: GEMINI_MODEL, shots });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Gemini shot-list generation failed.';
    const status = message.includes('not configured') ? 503 : 502;
    return res.status(status).json({ success: false, fallback: false, error: message });
  }
}
