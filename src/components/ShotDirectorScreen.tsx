import React from 'react';
import { AppScreen, MusicVideoProject, MusicVideoShot } from '../types';
import { Clapperboard, ArrowLeft, Play, Sparkles, Trash2, Plus, Clock, Video } from 'lucide-react';

interface ShotDirectorScreenProps {
  activeProject: MusicVideoProject;
  onUpdateProject: (proj: MusicVideoProject) => void;
  onNavigate: (screen: AppScreen) => void;
}

export const ShotDirectorScreen: React.FC<ShotDirectorScreenProps> = ({
  activeProject,
  onUpdateProject,
  onNavigate,
}) => {
  const handleDeleteShot = (id: string) => {
    const shots = activeProject.shots.filter(s => s.id !== id).map((s, idx) => ({ ...s, shotNumber: idx + 1 }));
    onUpdateProject({ ...activeProject, shots, updatedAt: Date.now() });
  };

  const handleStartProduction = () => {
    onNavigate('camera');
  };

  return (
    <div className="min-h-screen bg-[#121212] text-zinc-100 flex flex-col pb-24 select-none">
      {/* Top App Bar */}
      <header className="px-6 py-4 flex items-center justify-between border-b border-[#222222] bg-[#181818]/90 backdrop-blur-md sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <button
            onClick={() => onNavigate('songStudio')}
            className="p-2 rounded-2xl bg-[#1E1E1E] border border-[#2D2D2D] text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-bold text-base text-white">AI Shot Director</h1>
            <span className="text-[10px] text-blue-400 font-mono uppercase">Phase 2: Shot Planning</span>
          </div>
        </div>

        <button
          onClick={handleStartProduction}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs rounded-2xl shadow-lg shadow-blue-600/30"
        >
          <Play className="w-4 h-4 fill-current" />
          <span>Start Camera</span>
        </button>
      </header>

      {/* Main Content */}
      <main className="max-w-md mx-auto w-full p-6 space-y-6 flex-1">
        <div className="p-5 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] flex items-center justify-between">
          <div>
            <span className="text-[10px] font-mono text-blue-400 uppercase tracking-widest">{activeProject.shootingStyle}</span>
            <h3 className="font-bold text-white text-base">{activeProject.title}</h3>
            <p className="text-xs text-zinc-400">{activeProject.shots.length} Synchronized Shots • {activeProject.targetDuration}s</p>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-blue-600/20 text-blue-400 flex items-center justify-center font-bold">
            <Clapperboard className="w-5 h-5" />
          </div>
        </div>

        {/* Shot List */}
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400 px-1">Shot Sequence Timeline</h3>
          
          <div className="space-y-3">
            {activeProject.shots.map((shot) => (
              <div
                key={shot.id}
                className="p-4 rounded-3xl bg-[#181818] border border-[#252525] flex items-start justify-between gap-4"
              >
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 rounded-xl bg-[#222222] border border-[#2F2F2F] flex items-center justify-center text-xs font-bold text-blue-400 shrink-0">
                    {shot.shotNumber}
                  </div>
                  <div>
                    <h4 className="font-bold text-white text-sm mb-0.5">{shot.name}</h4>
                    <p className="text-xs text-zinc-400 mb-2">{shot.subjectAction}</p>
                    <div className="flex flex-wrap gap-2 text-[10px] text-zinc-500 font-mono">
                      <span className="px-2 py-0.5 rounded bg-[#222222] text-zinc-300">{shot.cameraAngle}</span>
                      <span className="px-2 py-0.5 rounded bg-[#222222] text-zinc-300">{shot.cameraMovement}</span>
                      <span className="px-2 py-0.5 rounded bg-[#222222] text-zinc-300">{shot.duration}s • {shot.suggestedFps}fps</span>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleDeleteShot(shot.id)}
                  className="p-2 text-zinc-500 hover:text-red-400 transition-colors"
                  title="Delete Shot"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Start Production Button */}
        <button
          onClick={handleStartProduction}
          className="w-full flex items-center justify-center gap-2 py-4 bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm rounded-2xl shadow-xl shadow-blue-600/30 transition-all active:scale-95"
        >
          <Video className="w-4 h-4" />
          <span>LAUNCH CAMERA DIRECTOR</span>
        </button>
      </main>
    </div>
  );
};
