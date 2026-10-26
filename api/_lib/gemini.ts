import { GoogleGenAI, Type } from '@google/genai';

const apiKey = process.env.GEMINI_API_KEY?.trim();

export function getGeminiClient() {
  if (!apiKey || ['MY_GEMINI_API_KEY', 'dummy-key'].includes(apiKey)) {
    throw new Error('Gemini is not configured. Add a valid GEMINI_API_KEY in Vercel Project Settings → Environment Variables, then redeploy.');
  }
  return new GoogleGenAI({ apiKey });
}

export const GEMINI_MODEL = 'gemini-3.8-flash';
export { Type };
