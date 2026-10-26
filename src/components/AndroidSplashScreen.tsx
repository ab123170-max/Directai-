import React, { useEffect } from 'react';
import { AppScreen } from '../types';
import { Video, Sparkles, Music } from 'lucide-react';

interface AndroidSplashScreenProps {
  onNavigate: (screen: AppScreen) => void;
}

export const AndroidSplashScreen: React.FC<AndroidSplashScreenProps> = ({ onNavigate }) => {
  useEffect(() => {
    const timer = setTimeout(() => onNavigate('feed'), 1600);
    return () => clearTimeout(timer);
  }, [onNavigate]);

  return (
    <div className="min-h-[100dvh] bg-black text-white flex flex-col items-center justify-center p-6 relative overflow-hidden select-none">
      <div className="absolute -left-24 top-1/4 h-72 w-72 rounded-full bg-cyan-400/20 blur-[100px]" />
      <div className="absolute -right-24 bottom-1/4 h-72 w-72 rounded-full bg-pink-500/20 blur-[100px]" />
      <div className="relative z-10 flex flex-col items-center text-center">
        <div className="relative mb-6 flex h-24 w-24 items-center justify-center rounded-[30px] bg-gradient-to-br from-cyan-400 via-white to-pink-500 shadow-2xl shadow-pink-500/20">
          <div className="flex h-[88px] w-[88px] items-center justify-center rounded-[26px] bg-black"><Video className="h-10 w-10 text-white" /></div>
          <div className="absolute -bottom-2 -right-2 flex h-9 w-9 items-center justify-center rounded-2xl border border-white/20 bg-pink-500"><Music className="h-4 w-4 text-white" /></div>
        </div>
        <h1 className="text-3xl font-extrabold tracking-tight">Cameraman <span className="text-cyan-300">AI</span></h1>
        <p className="mt-2 text-xs font-bold uppercase tracking-[0.25em] text-white/50">Create · Shoot · Share</p>
        <div className="mt-8 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs text-white/70">
          <Sparkles className="h-4 w-4 animate-pulse text-pink-400" /> Loading your creator feed…
        </div>
      </div>
    </div>
  );
};
