import React from 'react';
import { AppScreen, MusicVideoProject } from '../types';
import { Video, Music, Plus, Sparkles, FolderOpen, Settings, Play, ChevronRight, Clapperboard } from 'lucide-react';

interface AndroidHomeDashboardProps {
  projects: MusicVideoProject[];
  activeProject: MusicVideoProject;
  onSelectProject: (proj: MusicVideoProject) => void;
  onCreateNew: () => void;
  onNavigate: (screen: AppScreen) => void;
}

export const AndroidHomeDashboard: React.FC<AndroidHomeDashboardProps> = ({
  projects,
  activeProject,
  onSelectProject,
  onCreateNew,
  onNavigate,
}) => {
  return (
    <div className="min-h-screen bg-[#121212] text-zinc-100 flex flex-col pb-24 select-none">
      {/* Material You Top App Bar */}
      <header className="px-6 py-4 flex items-center justify-between border-b border-[#222222] bg-[#181818]/90 backdrop-blur-md sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-600/30">
            <Video className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-base tracking-tight text-white">Cameraman AI</h1>
            <span className="text-[10px] text-blue-400 font-mono uppercase">Smart Music Video Director</span>
          </div>
        </div>

        <button
          onClick={() => onNavigate('settings')}
          className="p-2.5 rounded-2xl bg-[#1E1E1E] border border-[#2D2D2D] text-zinc-400 hover:text-white transition-colors"
          title="Settings"
        >
          <Settings className="w-5 h-5" />
        </button>
      </header>

      {/* Main Content */}
      <main className="max-w-md mx-auto w-full p-6 space-y-6 flex-1">
        {/* Primary Action Card: CREATE MUSIC VIDEO */}
        <div className="p-6 rounded-3xl bg-gradient-to-br from-blue-600 via-indigo-600 to-blue-700 text-white relative overflow-hidden shadow-2xl shadow-blue-600/30">
          <div className="absolute right-0 top-0 w-64 h-64 bg-white/10 rounded-full blur-2xl pointer-events-none" />

          <div className="relative z-10">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/25 text-white text-xs font-semibold mb-4 backdrop-blur-md">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Autonomous AI Director</span>
            </div>

            <h2 className="text-2xl font-extrabold tracking-tight mb-2">Create Music Video</h2>
            <p className="text-xs text-blue-100 mb-6 leading-relaxed">
              Import your track, analyze beats, generate cinematic shot lists, and record with CameraX movement guidance.
            </p>

            <button
              onClick={onCreateNew}
              className="w-full flex items-center justify-center gap-2 py-4 px-6 bg-white text-blue-900 font-bold text-sm rounded-2xl shadow-xl transition-all hover:bg-blue-50 active:scale-95"
            >
              <Plus className="w-5 h-5" />
              <span>CREATE MUSIC VIDEO</span>
            </button>
          </div>
        </div>

        {/* Active Project Card */}
        {activeProject && (
          <div className="space-y-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400 px-1">Active Production</h3>
            <div 
              onClick={() => onNavigate('pipeline')}
              className="p-5 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] hover:border-blue-500/50 transition-all cursor-pointer flex items-center justify-between group"
            >
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-2xl bg-blue-500/10 border border-blue-500/20 text-blue-400 flex items-center justify-center font-bold">
                  <Clapperboard className="w-6 h-6" />
                </div>
                <div>
                  <span className="text-[10px] font-mono text-blue-400 uppercase tracking-widest">{activeProject.shootingStyle}</span>
                  <h4 className="font-bold text-white text-base">{activeProject.title}</h4>
                  <p className="text-xs text-zinc-400">{activeProject.song.title} • {activeProject.shots.filter(s => s.status === 'COMPLETED').length} / {activeProject.shots.length} shots</p>
                </div>
              </div>

              <div className="w-10 h-10 rounded-2xl bg-[#252525] flex items-center justify-center text-zinc-300 group-hover:bg-blue-600 group-hover:text-white transition-all">
                <ChevronRight className="w-5 h-5" />
              </div>
            </div>
          </div>
        )}

        {/* Recent Projects List */}
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400 px-1">Saved Projects</h3>
          <div className="space-y-2">
            {projects.map((proj) => (
              <div
                key={proj.id}
                onClick={() => onSelectProject(proj)}
                className="p-4 rounded-2xl bg-[#181818] border border-[#252525] flex items-center justify-between cursor-pointer hover:bg-[#202020] transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-[#222222] flex items-center justify-center text-zinc-400">
                    <Music className="w-4 h-4" />
                  </div>
                  <div>
                    <h5 className="font-bold text-sm text-white">{proj.title}</h5>
                    <span className="text-[11px] text-zinc-400">{proj.song.title} • {proj.shots.length} Shots</span>
                  </div>
                </div>
                <span className="text-xs text-blue-400 font-medium">Open</span>
              </div>
            ))}
          </div>
        </div>
      </main>

      {/* Material You Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-0 right-0 h-20 bg-[#161616] border-t border-[#222222] flex items-center justify-around px-6 z-40 max-w-md mx-auto">
        <button 
          onClick={() => onNavigate('home')} 
          className="flex flex-col items-center gap-1 text-blue-400"
        >
          <FolderOpen className="w-5 h-5" />
          <span className="text-[10px] font-medium">Projects</span>
        </button>

        <button 
          onClick={() => onNavigate('songStudio')} 
          className="flex flex-col items-center gap-1 text-zinc-400 hover:text-white transition-colors"
        >
          <Music className="w-5 h-5" />
          <span className="text-[10px] font-medium">Song Studio</span>
        </button>

        <button 
          onClick={() => onNavigate('camera')} 
          className="flex flex-col items-center gap-1 text-zinc-400 hover:text-white transition-colors"
        >
          <Video className="w-5 h-5" />
          <span className="text-[10px] font-medium">CameraX</span>
        </button>
      </nav>
    </div>
  );
};
