import React, { useState, useEffect, useRef } from 'react';
import { AppScreen, MusicVideoProject, MusicVideoShot } from '../types';
import { 
  Camera, Video, Square, Play, RotateCcw, Sparkles, Layers, Sliders, 
  Volume2, VolumeX, Grid, Compass, ArrowLeft, Check, ChevronRight, 
  Flashlight, ZoomIn, Eye, Film, Clapperboard, RefreshCw, CheckCircle2 
} from 'lucide-react';

interface NativeCameraScreenProps {
  activeProject: MusicVideoProject;
  onUpdateProject: (proj: MusicVideoProject) => void;
  onNavigate: (screen: AppScreen) => void;
}

export const NativeCameraScreen: React.FC<NativeCameraScreenProps> = ({
  activeProject,
  onUpdateProject,
  onNavigate,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const [stream, setStream] = useState<MediaStream | null>(null);
  const [currentShotIndex, setCurrentShotIndex] = useState(
    activeProject.shots.findIndex(s => s.status === 'READY_TO_RECORD' || s.status === 'WAITING') >= 0 
      ? activeProject.shots.findIndex(s => s.status === 'READY_TO_RECORD' || s.status === 'WAITING') 
      : 0
  );
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [reviewClipUrl, setReviewClipUrl] = useState<string | null>(null);

  const currentShot: MusicVideoShot = activeProject.shots[currentShotIndex] || activeProject.shots[0];

  useEffect(() => {
    let activeStream: MediaStream | null = null;
    async function initCamera() {
      try {
        setCameraError(null);
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          const s = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: { ideal: 'environment' }, width: { ideal: 1080 }, height: { ideal: 1920 } },
            audio: true,
          });
          activeStream = s;
          setStream(s);
          if (videoRef.current) {
            videoRef.current.srcObject = s;
          }
        } else {
          setCameraError('Camera API not available in browser.');
        }
      } catch (err: any) {
        console.error('Camera error:', err);
        setCameraError('Camera permission denied. Running in simulator mode.');
      }
    }
    initCamera();
    return () => {
      if (activeStream) {
        activeStream.getTracks().forEach(t => t.stop());
      }
    };
  }, []);

  useEffect(() => {
    let interval: any;
    if (isRecording) {
      interval = setInterval(() => {
        setRecordingSeconds(s => s + 1);
      }, 1000);
    } else {
      setRecordingSeconds(0);
    }
    return () => clearInterval(interval);
  }, [isRecording]);

  const handleStartRecording = () => {
    if (!stream) {
      setIsRecording(true);
      return;
    }
    chunksRef.current = [];
    try {
      const recorder = new MediaRecorder(stream, { mimeType: 'video/webm;codecs=vp9,opus' });
      mediaRecorderRef.current = recorder;
      recorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunksRef.current.push(e.data);
      };
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: 'video/webm' });
        const url = URL.createObjectURL(blob);
        setReviewClipUrl(url);
      };
      recorder.start();
      setIsRecording(true);
    } catch (e) {
      console.error(e);
      setIsRecording(true);
    }
  };

  const handleStopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      try { mediaRecorderRef.current.stop(); } catch (e) {}
    }
    setIsRecording(false);
    if (!mediaRecorderRef.current) {
      setReviewClipUrl('blob:simulator-clip');
    }
  };

  const handleApproveTake = () => {
    if (!reviewClipUrl) return;
    const updatedShots = activeProject.shots.map((s, idx) => {
      if (idx === currentShotIndex) {
        return {
          ...s,
          status: 'COMPLETED' as const,
          recordedClipUrl: reviewClipUrl,
          takes: [...s.takes, reviewClipUrl],
        };
      }
      if (idx === currentShotIndex + 1) {
        return { ...s, status: 'READY_TO_RECORD' as const };
      }
      return s;
    });

    onUpdateProject({
      ...activeProject,
      shots: updatedShots,
      updatedAt: Date.now(),
    });

    setReviewClipUrl(null);
    if (currentShotIndex + 1 < activeProject.shots.length) {
      setCurrentShotIndex(currentShotIndex + 1);
    } else {
      onNavigate('editing');
    }
  };

  return (
    <div className="fixed inset-0 bg-black text-white flex flex-col overflow-hidden select-none">
      {/* CameraX Top Bar */}
      <div className="absolute top-0 left-0 right-0 z-30 flex items-center justify-between px-6 py-4 bg-gradient-to-b from-black/80 via-black/40 to-transparent">
        <button
          onClick={() => onNavigate('home')}
          className="flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-black/60 border border-white/10 backdrop-blur-md text-xs font-semibold text-zinc-300"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Dashboard</span>
        </button>

        <div className="px-4 py-1.5 rounded-full bg-blue-600/30 border border-blue-500/50 backdrop-blur-md text-xs font-mono font-bold text-blue-300">
          SHOT {currentShotIndex + 1} OF {activeProject.shots.length}
        </div>

        <button
          onClick={() => onNavigate('editing')}
          className="px-3.5 py-1.5 rounded-full bg-black/60 border border-white/10 backdrop-blur-md text-xs font-semibold text-blue-400"
        >
          Editor
        </button>
      </div>

      {/* Camera Viewport */}
      <div className="relative flex-1 w-full h-full bg-[#0d0d0d] flex items-center justify-center overflow-hidden">
        {cameraError && (
          <div className="absolute inset-0 z-20 bg-zinc-900/90 flex flex-col items-center justify-center p-6 text-center">
            <Camera className="w-12 h-12 text-zinc-600 mb-3 animate-pulse" />
            <h4 className="font-bold text-base text-white mb-1">Camera Simulator Mode</h4>
            <p className="text-xs text-zinc-400 max-w-xs mb-6">{cameraError}</p>
            <button
              onClick={() => setCameraError(null)}
              className="px-6 py-2.5 bg-blue-600 text-white font-bold text-xs rounded-xl"
            >
              Dismiss
            </button>
          </div>
        )}

        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="absolute inset-0 w-full h-full object-cover"
        />

        {/* Framing Guides & Grid */}
        <div className="absolute inset-0 pointer-events-none grid grid-cols-3 grid-rows-3 border border-white/10">
          <div className="border-r border-b border-white/10" />
          <div className="border-r border-b border-white/10" />
          <div className="border-b border-white/10" />
          <div className="border-r border-b border-white/10" />
          <div className="border-r border-b border-white/10" />
          <div className="border-b border-white/10" />
          <div className="border-r border-white/10" />
          <div className="border-r border-white/10" />
          <div />
        </div>

        {/* AI Director Coaching Overlay */}
        {currentShot && (
          <div className="absolute top-20 left-6 right-6 z-20 bg-black/70 backdrop-blur-md border border-white/20 rounded-3xl p-4 shadow-2xl animate-fade-in">
            <div className="flex items-center justify-between mb-1">
              <span className="text-[10px] font-mono text-blue-400 uppercase tracking-widest">{currentShot.cameraAngle}</span>
              <span className="text-[10px] font-mono text-zinc-400">{currentShot.duration}s • {currentShot.suggestedFps}fps</span>
            </div>
            <h3 className="font-bold text-white text-base mb-1">{currentShot.name}</h3>
            <p className="text-xs text-zinc-300 mb-2">{currentShot.subjectAction}</p>
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-600/20 border border-blue-500/40 text-blue-300 text-xs font-semibold">
              <Compass className="w-3.5 h-3.5 animate-spin" />
              <span>Movement: {currentShot.cameraMovement}</span>
            </div>
          </div>
        )}

        {/* Recording Timer */}
        {isRecording && (
          <div className="absolute top-44 left-1/2 -translate-x-1/2 z-30 flex items-center gap-2 px-4 py-1.5 rounded-full bg-red-600 text-white font-mono text-xs shadow-xl animate-pulse">
            <span className="w-2.5 h-2.5 rounded-full bg-white animate-ping" />
            <span>RECORDING 00:{recordingSeconds.toString().padStart(2, '0')}</span>
          </div>
        )}
      </div>

      {/* Review Modal if Clip Recorded */}
      {reviewClipUrl && (
        <div className="absolute inset-0 z-50 bg-black/95 backdrop-blur-xl flex flex-col items-center justify-between p-6">
          <div className="w-full max-w-md text-center pt-4">
            <span className="text-xs font-mono text-blue-400 uppercase tracking-widest">Take Verification</span>
            <h3 className="font-bold text-white text-lg">{currentShot.name}</h3>
          </div>

          <div className="w-full max-w-sm aspect-[9/16] bg-zinc-900 rounded-3xl overflow-hidden border border-zinc-800 flex items-center justify-center relative">
            <video src={reviewClipUrl} controls autoPlay playsInline className="w-full h-full object-cover" />
          </div>

          <div className="w-full max-w-md flex items-center gap-3 pb-6">
            <button
              onClick={() => setReviewClipUrl(null)}
              className="flex-1 py-3.5 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-white font-bold text-xs rounded-2xl"
            >
              Retake Shot
            </button>
            <button
              onClick={handleApproveTake}
              className="flex-1 py-3.5 bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs rounded-2xl shadow-xl shadow-blue-600/30 flex items-center justify-center gap-2"
            >
              <Check className="w-4 h-4" />
              <span>Approve Take</span>
            </button>
          </div>
        </div>
      )}

      {/* Bottom Camera Controls Bar */}
      <div className="absolute bottom-0 left-0 right-0 z-30 p-8 bg-gradient-to-t from-black via-black/80 to-transparent flex items-center justify-around">
        <button
          onClick={() => {
            if (currentShotIndex > 0) setCurrentShotIndex(currentShotIndex - 1);
          }}
          className="w-12 h-12 rounded-2xl bg-zinc-900/80 border border-white/10 flex items-center justify-center text-zinc-300"
        >
          <ChevronRight className="w-5 h-5 rotate-180" />
        </button>

        {!isRecording ? (
          <button
            onClick={handleStartRecording}
            className="w-20 h-20 rounded-full bg-red-600 hover:bg-red-500 border-4 border-white/40 flex items-center justify-center text-white shadow-2xl shadow-red-600/50 transition-all active:scale-95"
            title="Start Recording"
          >
            <div className="w-8 h-8 rounded-full bg-white" />
          </button>
        ) : (
          <button
            onClick={handleStopRecording}
            className="w-20 h-20 rounded-full bg-zinc-900 border-4 border-red-600 flex items-center justify-center text-red-500 shadow-2xl shadow-red-600/50 transition-all active:scale-95 animate-pulse"
            title="Stop Recording"
          >
            <Square className="w-8 h-8 fill-current" />
          </button>
        )}

        <button
          onClick={() => {
            if (currentShotIndex + 1 < activeProject.shots.length) setCurrentShotIndex(currentShotIndex + 1);
          }}
          className="w-12 h-12 rounded-2xl bg-zinc-900/80 border border-white/10 flex items-center justify-center text-zinc-300"
        >
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
