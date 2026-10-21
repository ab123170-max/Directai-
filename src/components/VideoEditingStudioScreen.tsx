import React, { useState } from 'react';
import { AppScreen, MusicVideoProject } from '../types';
import { ArrowLeft, Film, Play, Music, Download, CheckCircle2, Sliders, Sparkles, Video } from 'lucide-react';

interface VideoEditingStudioScreenProps {
  activeProject: MusicVideoProject;
  onUpdateProject: (proj: MusicVideoProject) => void;
  onNavigate: (screen: AppScreen) => void;
}

export const VideoEditingStudioScreen: React.FC<VideoEditingStudioScreenProps> = ({
  activeProject,
  onUpdateProject,
  onNavigate,
}) => {
  const completedShots = activeProject.shots.filter(s => s.status === 'COMPLETED' && s.recordedClipUrl);
  const [selectedClip, setSelectedClip] = useState<string | null>(completedShots[0]?.recordedClipUrl || null);
  const [isExporting, setIsExporting] = useState(false);
  const [exportComplete, setExportComplete] = useState(false);

  const handleExport = () => {
    setIsExporting(true);
    setTimeout(() => {
      setIsExporting(false);
      setExportComplete(true);
    }, 2500);
  };

  return (
    <div className="min-h-screen bg-[#121212] text-zinc-100 flex flex-col pb-24 select-none">
      {/* Top App Bar */}
      <header className="px-6 py-4 flex items-center justify-between border-b border-[#222222] bg-[#181818]/90 backdrop-blur-md sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <button
            onClick={() => onNavigate('camera')}
            className="p-2 rounded-2xl bg-[#1E1E1E] border border-[#2D2D2D] text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-bold text-base text-white">Video Editing Studio</h1>
            <span className="text-[10px] text-blue-400 font-mono uppercase">Phase 3: Assembly & Export</span>
          </div>
        </div>

        <button
          onClick={handleExport}
          disabled={isExporting || completedShots.length === 0}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs rounded-2xl shadow-lg shadow-blue-600/30 disabled:opacity-50"
        >
          <Download className="w-4 h-4" />
          <span>{isExporting ? 'Rendering...' : 'Export Video'}</span>
        </button>
      </header>

      {/* Main Content */}
      <main className="max-w-md mx-auto w-full p-6 space-y-6 flex-1">
        {exportComplete && (
          <div className="p-4 rounded-3xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-between animate-fade-in">
            <div className="flex items-center gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <div>
                <h4 className="font-bold text-sm">Music Video Exported!</h4>
                <p className="text-xs text-emerald-300/80">Saved successfully to device storage.</p>
              </div>
            </div>
            <button onClick={() => setExportComplete(false)} className="text-xs underline">Dismiss</button>
          </div>
        )}

        {/* Video Preview Player */}
        <div className="w-full aspect-[9/16] max-h-[50vh] bg-zinc-900 rounded-3xl border border-[#252525] overflow-hidden relative flex items-center justify-center shadow-2xl mx-auto">
          {selectedClip ? (
            <video src={selectedClip} controls autoPlay playsInline className="w-full h-full object-cover" />
          ) : (
            <div className="text-center p-6">
              <Video className="w-12 h-12 text-zinc-700 mx-auto mb-3" />
              <h4 className="font-bold text-zinc-400 text-sm mb-1">No Completed Clips</h4>
              <p className="text-xs text-zinc-600">Record shots in CameraX studio to populate timeline.</p>
            </div>
          )}
        </div>

        {/* Soundtrack Info */}
        <div className="p-4 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-600/20 text-blue-400 flex items-center justify-center">
              <Music className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-white text-sm">{activeProject.song.title}</h4>
              <p className="text-xs text-zinc-400">{activeProject.song.artist} • {activeProject.song.bpm} BPM</p>
            </div>
          </div>
          <span className="text-[10px] font-mono px-2.5 py-1 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20">
            Synced
          </span>
        </div>

        {/* Captured Clips Strip */}
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400 px-1">Sequence Timeline ({completedShots.length})</h3>

          <div className="grid grid-cols-3 gap-3">
            {completedShots.map((shot) => {
              const isSelected = shot.recordedClipUrl === selectedClip;
              return (
                <div
                  key={shot.id}
                  onClick={() => setSelectedClip(shot.recordedClipUrl || null)}
                  className={`p-2 rounded-2xl border transition-all cursor-pointer flex flex-col justify-between ${
                    isSelected ? 'bg-[#222222] border-blue-500' : 'bg-[#181818] border-[#252525]'
                  }`}
                >
                  <div className="aspect-[9/16] bg-black rounded-xl overflow-hidden mb-2 relative flex items-center justify-center">
                    <video src={shot.recordedClipUrl} className="w-full h-full object-cover" />
                  </div>
                  <span className="text-[10px] font-mono text-zinc-400 truncate text-center">Shot {shot.shotNumber}</span>
                </div>
              );
            })}
          </div>
        </div>
      </main>
    </div>
  );
};
