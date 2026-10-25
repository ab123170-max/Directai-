import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Compass, Volume2, VolumeX, RefreshCw, ScanEye, ChevronDown, ChevronUp } from 'lucide-react';

type SceneAnalysis = {
  subject?: string;
  lighting?: string;
  composition?: string;
  stability?: string;
  direction?: string;
  movementSuggestion?: string;
  duration?: number;
  success?: boolean;
  fallback?: boolean;
};

type Props = {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  currentShot: string;
  isRecording: boolean;
};

const fallbackForShot = (shot: string): SceneAnalysis => {
  const s = shot.toLowerCase();
  if (s.includes('pan') || s.includes('scenic') || s.includes('landscape') || s.includes('vista')) {
    return {
      subject: 'Landscape / environment',
      lighting: 'Protect bright skies; tap the main scene to set exposure if available.',
      composition: 'Keep the horizon level and place it near the upper or lower third.',
      stability: 'Move slowly and keep your elbows close to your body.',
      direction: 'Pan slowly from left to right, then hold the final frame for 2 seconds.',
      movementSuggestion: 'pan-right',
      duration: 6,
    };
  }
  if (s.includes('reveal')) {
    return {
      subject: 'Foreground plus main subject',
      lighting: 'Avoid pointing directly into very bright light.',
      composition: 'Use a nearby object at the edge of the frame to reveal the background.',
      stability: 'Start still, then make one smooth, slow move.',
      direction: 'Begin with a tree, rock, or doorway partly blocking the view; slowly slide sideways to reveal the scene.',
      movementSuggestion: 'reveal',
      duration: 5,
    };
  }
  if (s.includes('close') || s.includes('detail') || s.includes('texture')) {
    return {
      subject: 'Small detail / texture',
      lighting: 'Use soft side light and avoid casting your phone shadow on the subject.',
      composition: 'Move closer only while focus remains sharp; leave a little breathing room.',
      stability: 'Brace your hands and pause before recording.',
      direction: 'Hold steady on one interesting detail for 4–5 seconds.',
      movementSuggestion: 'static',
      duration: 5,
    };
  }
  return {
    subject: 'Main subject in your scene',
    lighting: 'Keep the brightest area from washing out; use available natural light.',
    composition: 'Use the thirds grid and keep the horizon level.',
    stability: 'Hold with both hands; start and finish each move with a brief pause.',
    direction: 'Frame the main subject, move slowly in one direction, and hold the ending for 2 seconds.',
    movementSuggestion: 'static',
    duration: 5,
  };
};

export const LiveGuidancePanel: React.FC<Props> = ({ videoRef, currentShot, isRecording }) => {
  const [analysis, setAnalysis] = useState<SceneAnalysis>(() => fallbackForShot(currentShot));
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [voiceEnabled, setVoiceEnabled] = useState(true);
  const [expanded, setExpanded] = useState(true);
  const [sensorText, setSensorText] = useState('Motion guide ready');
  const [lastUpdated, setLastUpdated] = useState('Shot-based guidance');
  const [errorText, setErrorText] = useState('');
  const lastSpokenRef = useRef('');

  const speak = useCallback((text: string) => {
    if (!voiceEnabled || !('speechSynthesis' in window) || !text || lastSpokenRef.current === text) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 0.92;
    utterance.pitch = 1;
    utterance.lang = 'en-US';
    window.speechSynthesis.speak(utterance);
    lastSpokenRef.current = text;
  }, [voiceEnabled]);

  const analyzeFrame = useCallback(async (announce = false) => {
    const video = videoRef.current;
    if (!video || video.readyState < 2 || !video.videoWidth || !video.videoHeight) {
      setErrorText('Waiting for live camera preview…');
      return;
    }
    setIsAnalyzing(true);
    setErrorText('');
    try {
      const canvas = document.createElement('canvas');
      const scale = Math.min(1, 640 / video.videoWidth);
      canvas.width = Math.round(video.videoWidth * scale);
      canvas.height = Math.round(video.videoHeight * scale);
      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('Frame capture is unavailable.');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      const imageBase64 = canvas.toDataURL('image/jpeg', 0.58);
      const response = await fetch('/api/ai/analyze-scene', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageBase64, mode: 'travel', currentShot }),
      });
      if (!response.ok) throw new Error('Live AI service is not connected to this build.');
      const data = await response.json() as SceneAnalysis;
      if (!data.success && !data.direction) throw new Error('Scene analysis did not return guidance.');
      setAnalysis(data);
      setLastUpdated(data.fallback ? 'Smart guidance · no AI key' : 'Live scene analysis');
      if (announce && data.direction) speak(data.direction);
    } catch (error) {
      setAnalysis(fallbackForShot(currentShot));
      setLastUpdated('Shot-based guidance');
      setErrorText('Live AI analysis unavailable; showing useful guidance for this shot.');
      if (announce) speak(fallbackForShot(currentShot).direction || '');
    } finally {
      setIsAnalyzing(false);
    }
  }, [currentShot, speak, videoRef]);

  useEffect(() => {
    setAnalysis(fallbackForShot(currentShot));
    lastSpokenRef.current = '';
  }, [currentShot]);

  useEffect(() => {
    const onOrientation = (event: DeviceOrientationEvent) => {
      const gamma = event.gamma ?? 0;
      const beta = event.beta ?? 0;
      if (Math.abs(gamma) > 14) setSensorText(gamma > 0 ? 'Tilted right — level the phone' : 'Tilted left — level the phone');
      else if (beta < -25) setSensorText('Phone tilted too far up');
      else if (beta > 70) setSensorText('Phone tilted down — check your framing');
      else setSensorText('Phone angle looks steady');
    };
    window.addEventListener('deviceorientation', onOrientation);
    return () => window.removeEventListener('deviceorientation', onOrientation);
  }, []);

  useEffect(() => {
    if (!isRecording) return;
    const timer = window.setInterval(() => { void analyzeFrame(false); }, 7000);
    return () => window.clearInterval(timer);
  }, [analyzeFrame, isRecording]);

  useEffect(() => () => {
    if ('speechSynthesis' in window) window.speechSynthesis.cancel();
  }, []);

  const movement = (analysis.movementSuggestion || 'static').toLowerCase();
  const arrow = movement.includes('left') ? '←' : movement.includes('right') || movement.includes('pan') ? '→' : movement.includes('up') || movement.includes('tilt') ? '↑' : movement.includes('push') || movement.includes('closer') ? '⊙' : movement.includes('pull') || movement.includes('back') ? '↓' : movement.includes('orbit') ? '⟳' : movement.includes('reveal') ? '↗' : '●';

  return (
    <section className="absolute bottom-28 left-3 right-3 z-20 max-w-lg mx-auto rounded-2xl border border-emerald-300/30 bg-black/80 backdrop-blur-xl text-white shadow-2xl overflow-hidden">
      <div className="flex items-center justify-between gap-2 px-3 py-2 border-b border-white/10">
        <div className="flex items-center gap-2 min-w-0">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse shrink-0" />
          <div className="min-w-0">
            <p className="text-[11px] font-bold tracking-wide">LIVE CAMERA COACH</p>
            <p className="text-[9px] text-zinc-400 truncate">{lastUpdated}</p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button type="button" onClick={() => setVoiceEnabled(v => !v)} aria-label={voiceEnabled ? 'Turn voice guidance off' : 'Turn voice guidance on'} className="w-9 h-9 rounded-xl bg-white/10 flex items-center justify-center">
            {voiceEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
          </button>
          <button type="button" onClick={() => void analyzeFrame(true)} disabled={isAnalyzing} aria-label="Analyze live scene" className="w-9 h-9 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center disabled:opacity-50">
            {isAnalyzing ? <RefreshCw className="w-4 h-4 animate-spin" /> : <ScanEye className="w-4 h-4" />}
          </button>
          <button type="button" onClick={() => setExpanded(v => !v)} aria-label={expanded ? 'Collapse guidance' : 'Expand guidance'} className="w-9 h-9 rounded-xl bg-white/10 flex items-center justify-center">
            {expanded ? <ChevronDown className="w-4 h-4" /> : <ChevronUp className="w-4 h-4" />}
          </button>
        </div>
      </div>
      <div className="px-3 py-2 flex items-center gap-3">
        <div className="w-12 h-12 rounded-xl bg-emerald-400/10 border border-emerald-300/30 flex items-center justify-center text-3xl text-emerald-300 shrink-0" aria-hidden="true">{arrow}</div>
        <div className="min-w-0 flex-1">
          <p className="text-[10px] uppercase tracking-widest text-emerald-300">Do this now</p>
          <p className="text-xs leading-relaxed font-semibold">{analysis.direction || 'Hold steady and compose your shot.'}</p>
        </div>
      </div>
      {expanded && (
        <div className="px-3 pb-3 space-y-2">
          <div className="grid grid-cols-2 gap-2">
            <div className="rounded-xl bg-white/5 p-2">
              <p className="text-[9px] text-zinc-400 uppercase tracking-wider">Composition</p>
              <p className="text-[10px] leading-relaxed">{analysis.composition || 'Use the thirds grid; keep the horizon level.'}</p>
            </div>
            <div className="rounded-xl bg-white/5 p-2">
              <p className="text-[9px] text-zinc-400 uppercase tracking-wider">Light & stability</p>
              <p className="text-[10px] leading-relaxed">{analysis.lighting || 'Keep exposure balanced.'} {analysis.stability || ''}</p>
            </div>
          </div>
          <div className="flex items-center justify-between gap-2 text-[9px] text-zinc-400">
            <span className="flex items-center gap-1"><Compass className="w-3 h-3" /> {sensorText}</span>
            <span>{analysis.duration || 5}s suggested</span>
          </div>
          {errorText && <p className="text-[9px] text-amber-200/90">{errorText}</p>}
        </div>
      )}
    </section>
  );
};
