import express from 'express';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI, Type } from '@google/genai';
import dotenv from 'dotenv';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

dotenv.config();

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const apiKey = process.env.GEMINI_API_KEY;
const ai = new GoogleGenAI({
  apiKey: apiKey || 'dummy-key',
  httpOptions: {
    headers: {
      'User-Agent': 'aistudio-build',
    },
  },
});

async function startServer() {
  const app = express();
  app.use(express.json({ limit: '50mb' }));

  app.get('/api/health', (_req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  app.get('/api/ai/health', async (_req, res) => {
    if (!apiKey || ['MY_GEMINI_API_KEY', 'dummy-key'].includes(apiKey)) {
      return res.status(503).json({ ok: false, provider: 'gemini', reason: 'GEMINI_API_KEY is missing or is a placeholder.' });
    }
    const startedAt = Date.now();
    try {
      const response = await ai.models.generateContent({
        model: 'gemini-3.8-flash',
        contents: 'Reply with exactly: GEMINI_OK',
      });
      const text = (response.text || '').trim();
      if (!text) throw new Error('Gemini returned an empty response.');
      return res.json({ ok: true, provider: 'gemini', model: 'gemini-3.8-flash', latencyMs: Date.now() - startedAt, response: text.slice(0, 100) });
    } catch (error: any) {
      console.error('Gemini health check failed:', error);
      return res.status(502).json({ ok: false, provider: 'gemini', model: 'gemini-3.8-flash', reason: error?.message || 'Gemini request failed.' });
    }
  });

  app.post('/api/ai/analyze-scene', async (req, res) => {
    try {
      const { imageBase64, mode, currentShot } = req.body;

      if (!apiKey || ['MY_GEMINI_API_KEY', 'dummy-key'].includes(apiKey)) {
        return res.status(503).json({ success: false, fallback: false, error: 'Gemini is not configured. Add GEMINI_API_KEY to the server environment.' });
      }

      const parts: any[] = [];
      if (imageBase64) {
        const base64Data = imageBase64.replace(/^data:image\/\w+;base64,/, '');
        parts.push({
          inlineData: {
            mimeType: 'image/jpeg',
            data: base64Data,
          },
        });
      }

      parts.push({
        text: `You are an expert AI Camera Director for professional video production.
        Analyze the current camera scene for shooting mode "${mode || 'general'}" (current shot: ${currentShot || 'Establishing Shot'}).
        Provide JSON response with:
        - subject: detected subject description
        - lighting: lighting quality assessment and adjustment tip
        - composition: composition feedback based on rule of thirds
        - stability: camera stability advice
        - direction: specific actionable coaching instruction (e.g. "Move slightly to the left", "Slow push-in toward the subject", "Lower camera angle for drama")
        - movementSuggestion: recommended camera movement type (e.g. "push-in", "pan-right", "orbit", "static")
        - duration: suggested duration in seconds (integer)`,
      });

      const response = await ai.models.generateContent({
        model: 'gemini-3.8-flash',
        contents: { parts },
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              subject: { type: Type.STRING },
              lighting: { type: Type.STRING },
              composition: { type: Type.STRING },
              stability: { type: Type.STRING },
              direction: { type: Type.STRING },
              movementSuggestion: { type: Type.STRING },
              duration: { type: Type.INTEGER },
            },
            required: ['subject', 'lighting', 'composition', 'stability', 'direction', 'movementSuggestion', 'duration'],
          },
        },
      });

      const result = JSON.parse(response.text || '{}');
      res.json({ success: true, fallback: false, model: 'gemini-3.8-flash', ...result });
    } catch (error: any) {
      console.error('AI Scene Analysis error:', error);
      res.status(502).json({ success: false, fallback: false, error: error.message || 'Gemini scene analysis failed.' });
    }
  });

  app.post('/api/ai/generate-shotlist', async (req, res) => {
    try {
      const { title, category, description } = req.body;

      if (!apiKey || ['MY_GEMINI_API_KEY', 'dummy-key'].includes(apiKey)) {
        return res.status(503).json({ success: false, fallback: false, error: 'Gemini is not configured. Add GEMINI_API_KEY to the server environment.' });
      }

      const prompt = `Generate a professional cinematic shot list for a video project titled "${title || 'Untitled Project'}", category "${category || 'general'}", description "${description || 'Professional video'}".
      Provide an array of 8 to 12 distinct shots. Each shot must have:
      - shotNumber (integer starting at 1)
      - name (short shot name)
      - type (establishing, wide, medium, close-up, extreme-close-up, overhead, low-angle, tracking, reveal, detail, b-roll, hero, reaction, ending)
      - purpose (why this shot is needed)
      - angle (Eye-Level, Low-Angle, High-Angle, Overhead, Dutch Tilt)
      - movement (static, pan-left, pan-right, tilt-up, tilt-down, push-in, pull-back, orbit, track, reveal)
      - duration (recommended duration in seconds, e.g., 4)
      - subject (what is framed)`;

      const response = await ai.models.generateContent({
        model: 'gemini-3.8-flash',
        contents: prompt,
        config: {
          responseMimeType: 'application/json',
          responseSchema: {
            type: Type.ARRAY,
            items: {
              type: Type.OBJECT,
              properties: {
                shotNumber: { type: Type.INTEGER },
                name: { type: Type.STRING },
                type: { type: Type.STRING },
                purpose: { type: Type.STRING },
                angle: { type: Type.STRING },
                movement: { type: Type.STRING },
                duration: { type: Type.INTEGER },
                subject: { type: Type.STRING },
              },
              required: ['shotNumber', 'name', 'type', 'purpose', 'angle', 'movement', 'duration', 'subject'],
            },
          },
        },
      });

      const shots = JSON.parse(response.text || '[]');
      res.json({ success: true, fallback: false, model: 'gemini-3.8-flash', shots });
    } catch (error: any) {
      console.error('Shot list generation error:', error);
      res.status(502).json({ success: false, fallback: false, error: error?.message || 'Gemini shot-list generation failed.' });
    }
  });

  const distPath = path.resolve(__dirname, 'dist');
  if (process.env.NODE_ENV === 'production' && fs.existsSync(distPath)) {
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.resolve(distPath, 'index.html'));
    });
  } else {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  const PORT = process.env.PORT || 3000;
  app.listen(Number(PORT), '0.0.0.0', () => {
    console.log(`Server running on http://0.0.0.0:${PORT}`);
  });
}

function getFallbackRecommendations(mode: string) {
  const map: Record<string, any> = {
    food: {
      subject: 'Fresh Culinary Dish',
      lighting: 'Warm side-lighting enhances texture.',
      composition: 'Rule of thirds centered on steam and garnishes.',
      stability: 'Very stable. Hold framing.',
      direction: 'Slow push-in toward the plate center to highlight ingredients.',
      movementSuggestion: 'push-in',
      duration: 5,
    },
    product: {
      subject: 'Hero Product Showcase',
      lighting: 'Clean studio rim lighting. No harsh shadows.',
      composition: 'Centered composition with clean negative space.',
      stability: 'Steady tripod shot recommended.',
      direction: 'Orbit slowly around the product to reveal surface reflections.',
      movementSuggestion: 'orbit',
      duration: 6,
    },
    travel: {
      subject: 'Scenic Landscape & Atmosphere',
      lighting: 'Golden hour natural sunlight.',
      composition: 'Wide horizon positioned at the upper third.',
      stability: 'Smooth panning motion required.',
      direction: 'Pan smoothly from left to right across the horizon.',
      movementSuggestion: 'pan-right',
      duration: 8,
    },
    portrait: {
      subject: 'Expressive Subject Portrait',
      lighting: 'Soft softbox light on face.',
      composition: 'Eye-level framing with slight background blur.',
      stability: 'Hold camera steady at eye level.',
      direction: 'Hold a static hero shot while subject looks toward lens.',
      movementSuggestion: 'static',
      duration: 5,
    }
  };
  return map[mode] || {
    subject: 'Primary Subject in Frame',
    lighting: 'Natural ambient lighting is adequate.',
    composition: 'Keep subject centered within grid guides.',
    stability: 'Keep hands steady.',
    direction: 'Hold camera stable and record 4 seconds of clean B-roll.',
    movementSuggestion: 'static',
    duration: 4,
  };
}

function getFallbackShotList(_category: string) {
  return [
    { shotNumber: 1, name: 'Establishing Wide Shot', type: 'Establishing shot', purpose: 'Set the scene, location, and atmosphere.', angle: 'Eye-Level', movement: 'static', duration: 6, subject: 'Full environment overview' },
    { shotNumber: 2, name: 'Subject Medium Shot', type: 'Medium shot', purpose: 'Introduce the main subject or action.', angle: 'Eye-Level', movement: 'push-in', duration: 5, subject: 'Main subject upper body' },
    { shotNumber: 3, name: 'Texture Detail Close-up', type: 'Close-up', purpose: 'Highlight critical details, quality, and texture.', angle: 'Low-Angle', movement: 'static', duration: 4, subject: 'Key feature / ingredient / texture' },
    { shotNumber: 4, name: 'Action B-Roll Sequence', type: 'B-roll', purpose: 'Capture dynamic motion and process.', angle: 'Eye-Level', movement: 'track', duration: 5, subject: 'Active handling in progress' },
    { shotNumber: 5, name: 'Hero Climax Shot', type: 'Hero shot', purpose: 'Deliver the most striking visual frame of the sequence.', angle: 'Low-Angle', movement: 'orbit', duration: 6, subject: 'Completed masterpiece / product' },
    { shotNumber: 6, name: 'Cinematic Closing Shot', type: 'Ending shot', purpose: 'Provide a satisfying conclusion to the video sequence.', angle: 'Eye-Level', movement: 'pull-back', duration: 5, subject: 'Wider concluding view' }
  ];
}

startServer();
