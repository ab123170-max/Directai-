import { GoogleGenAI } from "@google/genai";

export default async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  if (req.method !== "GET") {
    res.setHeader("Allow", "GET");
    return res.status(405).json({ ok: false, error: "Method not allowed" });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || /MY_GEMINI_API_KEY|your[_ -]?api[_ -]?key|placeholder/i.test(apiKey)) {
    return res.status(503).json({
      ok: false,
      provider: "gemini",
      error: "GEMINI_API_KEY is missing or is still a placeholder. Add it in Vercel Project Settings > Environment Variables."
    });
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const model = process.env.GEMINI_MODEL || "gemini-2.5-flash";
    const result = await ai.models.generateContent({
      model,
      contents: "Reply with exactly: GEMINI_OK"
    });
    const text = result.text || "";
    return res.status(200).json({
      ok: text.includes("GEMINI_OK"),
      provider: "gemini",
      model,
      response: text.slice(0, 80),
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    return res.status(502).json({
      ok: false,
      provider: "gemini",
      error: error instanceof Error ? error.message : "Gemini request failed",
      timestamp: new Date().toISOString()
    });
  }
}
