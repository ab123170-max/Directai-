import React, { useState, useEffect, useRef } from 'react';
import { AppScreen, MusicVideoProject, MusicVideoShot } from '../types';
import {
  Camera, Square, ArrowLeft, Check, ChevronRight, Video, RefreshCw, Volume2, VolumeX
} from 'lucide-react';
import { LiveGuidancePanel } from './LiveGuidancePanel';

interface NativeCameraScreenProps {
  activeProject: MusicVideoProject;
  onUpdateProject: (proj: MusicVideoProject) => void;
  onNavigate: (screen: AppScreen) => void;
}

export const NativeCameraScreen: React.FC<NativeCameraScreenProps> = ({
  activeProject, onUpdateProject, onNavigate,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [currentShotIndex, setCurrentShotIndex] = useState(Math.max(0, activeProject.shots.findIndex(s => s.status === 'READY_TO_RECORD' || s.status === 'WAITING')));
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [cameraError, setCameraError] = useState('');
  const [reviewClipUrl, setReviewClipUrl] = useState<string | null>(null);
  const [voiceHints, setVoiceHints] = useState(true);
  const currentShot: MusicVideoShot | undefined = activeProject.shots[currentShotIndex];

  useEffect(() => {
    let cancelled = false;
    async function initCamera() {
      try {
        setCameraError('');
        if (!navigator.mediaDevices?.getUserMedia) throw new Error('This app cannot access the camera in the current environment. Open the installed app or a secure HTTPS page.');
        const s = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 }, frameRate: { ideal: 30, max: 60 } },
          audio: true,
        });
        if (cancelled) { s.getTracks().forEach(t => t.stop()); return; }
        streamRef.current = s;
        setStream(s);
        if (videoRef.current) {
          videoRef.current.srcObject = s;
          await videoRef.current.play().catch(() => undefined);
        }
      } catch (err) {
        setCameraError(err instanceof Error ? err.message : 'Camera permission denied. Allow camera and microphone access, then retry.');
      }
    }
    void initCamera();
    return () => {
      cancelled = true;
      streamRef.current?.getTracks().forEach(t => t.stop());
      streamRef.current = null;
      if (videoRef.current) videoRef.current.srcObject = null;
      if ('speechSynthesis' in window) window.speechSynthesis.cancel();
      if (reviewClipUrl) URL.revokeObjectURL(reviewClipUrl);
    };
  }, []);

  useEffect(() => {
    if (videoRef.current && stream && videoRef.current.srcObject !== stream) {
      videoRef.current.srcObject = stream;
      void videoRef.current.play().catch(() => undefined);
    }
  }, [stream]);

  useEffect(() => {
    if (!isRecording) { setRecordingSeconds(0); return; }
    const timer = window.setInterval(() => setRecordingSeconds(s => s + 1), 1000);
    return () => window.clearInterval(timer);
  }, [isRecording]);

  const handleStartRecording = () => {
    if (!stream) { setCameraError('Camera is not ready yet. Tap Retry camera before recording.'); return; }
    if (typeof MediaRecorder === 'undefined') { setCameraError('Video recording is not supported by this device browser.'); return; }
    const mimeType = ['video/mp4;codecs=h264,aac', 'video/webm;codecs=vp8,opus', 'video/webm'].find(type => MediaRecorder.isTypeSupported(type));
    if (!mimeType) { setCameraError('No compatible video recording format is available on this device.'); return; }
    chunksRef.current = [];
    try {
      const recorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = recorder;
      recorder.ondataavailable = event => { if (event.data?.size) chunksRef.current.push(event.data); };
      recorder.onerror = () => setCameraError('Recording failed. Please try again.');
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || mimeType });
        const url = URL.createObjectURL(blob);
        setReviewClipUrl(url);
      };
      recorder.start(500);
      setIsRecording(true);
      setCameraError('');
    } catch {
      setCameraError('Unable to start recording. Try a lower resolution or restart the camera.');
    }
  };

  const handleStopRecording = () => {
    const recorder = mediaRecorderRef.current;
    if (recorder && recorder.state !== 'inactive') recorder.stop();
    setIsRecording(false);
  };

  const handleApproveTake = () => {
    if (!reviewClipUrl || !currentShot) return;
    const updatedShots = activeProject.shots.map((shot, index) => {
      if (index === currentShotIndex) return { ...shot, status: 'COMPLETED' as const, recordedClipUrl: reviewClipUrl, takes: [...shot.takes, reviewClipUrl] };
      if (index === currentShotIndex + 1) return { ...shot, status: 'READY_TO_RECORD' as const };
      return shot;
    });
    onUpdateProject({ ...activeProject, shots: updatedShots, updatedAt: Date.now() });
    setReviewClipUrl(null);
    if (currentShotIndex + 1 < activeProject.shots.length) setCurrentShotIndex(currentShotIndex + 1);
    else onNavigate('editing');
  };

  const handleRetake = () => {
    if (reviewClipUrl) URL.revokeObjectURL(reviewClipUrl);
    setReviewClipUrl(null);
  };

  return (
    <div className="fixed inset-0 bg-black text-white flex flex-col overflow-hidden select-none">
      <header className="absolute top-0 left-0 right-0 z-30 flex items-center justify-between px-3 py-3 bg-gradient-to-b from-black/80 to-transparent">
        <button onClick={() => onNavigate('home')} className="flex items-center gap-1.5 px-3 py-2 rounded-full bg-black/60 border border-white/10 text-xs font-semibold"><ArrowLeft className="w-4 h-4" /> Home</button>
        <div className="px-3 py-2 rounded-full bg-blue-600/30 border border-blue-500/50 text-[10px] font-mono font-bold text-blue-200">SHOT {Math.min(currentShotIndex + 1, activeProject.shots.length)} / {activeProject.shots.length}</div>
        <button onClick={() => onNavigate('editing')} className="px-3 py-2 rounded-full bg-black/60 border border-white/10 text-xs font-semibold text-blue-300">Editor</button>
      </header>

      <main className="relative flex-1 w-full min-h-0 bg-[#080b0d] flex items-center justify-center overflow-hidden">
        <video ref={videoRef} autoPlay playsInline muted className="absolute inset-0 w-full h-full object-cover" />
        <div className="absolute inset-0 pointer-events-none grid grid-cols-3 grid-rows-3 border border-white/20">
          {Array.from({ length: 9 }, (_, i) => <div key={i} className={(i % 3 !== 2 ? 'border-r ' : '') + (i < 6 ? 'border-b ' : '') + 'border-white/25'} />)}
        </div>
        <div className="absolute top-16 left-3 right-3 z-10 max-w-lg mx-auto rounded-xl bg-black/65 backdrop-blur-md border border-white/15 px-3 py-2">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[9px] text-blue-300 uppercase tracking-widest">Current shot</p>
              <p className="text-xs font-bold truncate">{currentShot?.name || 'Create a shot plan first'}</p>
              <p className="text-[10px] text-zinc-300 truncate">{currentShot?.cameraAngle || 'Wide'} · {currentShot?.cameraMovement || 'Hold steady'} · {currentShot?.suggestedFps || 30} fps</p>
            </div>
            <button type="button" onClick={() => { setVoiceHints(v => !v); if ('speechSynthesis' in window) window.speechSynthesis.cancel(); }} aria-label={voiceHints ? 'Mute voice hints' : 'Enable voice hints'} className="w-9 h-9 shrink-0 rounded-xl bg-white/10 flex items-center justify-center">{voiceHints ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}</button>
          </div>
        </div>

        <LiveGuidancePanel videoRef={videoRef} currentShot={currentShot ? [currentShot.name, currentShot.cameraMovement, currentShot.framingInstructions].join('. ') : 'Scenic establishing shot'} isRecording={isRecording} />

        {isRecording && <div className="absolute top-32 left-1/2 -translate-x-1/2 z-30 flex items-center gap-2 px-3 py-1.5 rounded-full bg-red-600 text-white font-mono text-xs"><span className="w-2 h-2 rounded-full bg-white animate-pulse" /> REC {String(Math.floor(recordingSeconds / 60)).padStart(2, '0')}:{String(recordingSeconds % 60).padStart(2, '0')}</div>}

        {cameraError && <div className="absolute inset-x-4 top-1/2 -translate-y-1/2 z-40 rounded-2xl border border-amber-300/30 bg-black/90 p-4 text-center">
          <Camera className="w-8 h-8 mx-auto mb-2 text-amber-300" />
          <p className="text-sm font-bold mb-1">Camera needs attention</p>
          <p className="text-xs text-zinc-300 mb-3">{cameraError}</p>
          <button type="button" onClick={() => { setCameraError(''); setStream(null); const s = streamRef.current; s?.getTracks().forEach(t => t.stop()); streamRef.current = null; window.location.reload(); }} className="px-4 py-2 rounded-xl bg-blue-600 text-xs font-bold"><RefreshCw className="inline w-3 h-3 mr-1" /> Retry camera</button>
        </div>}
      </main>

      {reviewClipUrl && <div className="absolute inset-0 z-50 bg-black/95 backdrop-blur-xl flex flex-col items-center justify-between p-4">
        <div className="text-center pt-4"><span className="text-[10px] text-blue-300 uppercase tracking-widest">Take review</span><h3 className="font-bold text-lg">{currentShot?.name || 'Recorded clip'}</h3></div>
        <div className="w-full max-w-sm flex-1 min-h-0 my-4 bg-zinc-900 rounded-2xl overflow-hidden border border-zinc-800"><video src={reviewClipUrl} controls autoPlay playsInline className="w-full h-full object-contain" /></div>
        <div className="w-full max-w-md flex gap-3 pb-4">
          <button onClick={handleRetake} className="flex-1 py-3.5 bg-zinc-900 border border-zinc-700 text-white font-bold text-xs rounded-2xl">Retake</button>
          <button onClick={handleApproveTake} className="flex-1 py-3.5 bg-blue-600 text-white font-bold text-xs rounded-2xl flex items-center justify-center gap-2"><Check className="w-4 h-4" /> Approve take</button>
        </div>
      </div>}

      <footer className="absolute bottom-0 left-0 right-0 z-30 p-4 pt-10 bg-gradient-to-t from-black via-black/85 to-transparent flex items-center justify-around">
        <button type="button" disabled={isRecording || currentShotIndex <= 0} onClick={() => setCurrentShotIndex(i => Math.max(0, i - 1))} aria-label="Previous shot" className="w-12 h-12 rounded-2xl bg-zinc-900/90 border border-white/10 flex items-center justify-center disabled:opacity-30"><ChevronRight className="w-5 h-5 rotate-180" /></button>
        {!isRecording ? <button type="button" onClick={handleStartRecording} disabled={!stream || !currentShot} aria-label="Start recording" className="w-16 h-16 rounded-full bg-red-600 border-4 border-white/60 flex items-center justify-center shadow-xl disabled:opacity-40"><span className="w-7 h-7 rounded-full bg-white" /></button> : <button type="button" onClick={handleStopRecording} aria-label="Stop recording" className="w-16 h-16 rounded-full bg-zinc-900 border-4 border-red-500 flex items-center justify-center text-red-400 shadow-xl"><Square className="w-7 h-7 fill-current" /></button>}
        <button type="button" disabled={isRecording || currentShotIndex + 1 >= activeProject.shots.length} onClick={() => setCurrentShotIndex(i => Math.min(activeProject.shots.length - 1, i + 1))} aria-label="Next shot" className="w-12 h-12 rounded-2xl bg-zinc-900/90 border border-white/10 flex items-center justify-center disabled:opacity-30"><ChevronRight className="w-5 h-5" /></button>
      </footer>
    </div>
  );
};
