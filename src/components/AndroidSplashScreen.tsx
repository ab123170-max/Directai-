import React, { useEffect } from 'react';
import { AppScreen } from '../types';
import { Video, Sparkles, Music } from 'lucide-react';

interface AndroidSplashScreenProps {
  onNavigate: (screen: AppScreen) => void;
}

export const AndroidSplashScreen: React.FC<AndroidSplashScreenProps> = ({ onNavigate }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onNavigate('home');
    }, 2200);
    return () => clearTimeout(timer);
  }, [onNavigate]);

  return (
    <div className="min-h-screen bg-[#121212] text-white flex flex-col items-center justify-center p-6 relative overflow-hidden select-none">
      {/* Background Ambient Glow */}
      <div className="absolute w-96 h-96 bg-blue-600/15 rounded-full blur-[120px] pointer-events-none" />

      <div className="flex flex-col items-center text-center z-10 animate-fade-in">
        <div className="w-24 h-24 rounded-3xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-2xl shadow-blue-600/40 mb-6 relative">
          <Video className="w-10 h-10 text-white" />
          <div className="absolute -bottom-1 -right-1 w-8 h-8 rounded-2xl bg-[#1E1E1E] border border-[#2D2D2D] flex items-center justify-center text-blue-400">
            <Music className="w-4 h-4" />
          </div>
        </div>

        <h1 className="text-3xl font-extrabold tracking-tight text-white mb-2 font-['Plus_Jakarta_Sans',sans-serif]">
          Cameraman AI
        </h1>
        <p className="text-xs font-mono text-blue-400 tracking-widest uppercase mb-8">
          Smart Music Video Director
        </p>

        <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-[#1E1E1E] border border-[#2D2D2D] text-xs text-zinc-400">
          <Sparkles className="w-3.5 h-3.5 text-blue-400 animate-pulse" />
          <span>Initializing CameraX & Audio Engine...</span>
        </div>
      </div>
    </div>
  );
};
