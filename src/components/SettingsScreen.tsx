import React from 'react';
import { AppScreen } from '../types';
import { ArrowLeft, Camera, Mic, Shield, Sparkles, HardDrive } from 'lucide-react';

interface SettingsScreenProps {
  onNavigate: (screen: AppScreen) => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onNavigate }) => {
  return (
    <div className="min-h-screen bg-[#121212] text-zinc-100 flex flex-col pb-24 select-none">
      {/* Top App Bar */}
      <header className="px-6 py-4 flex items-center justify-between border-b border-[#222222] bg-[#181818]/90 backdrop-blur-md sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <button
            onClick={() => onNavigate('home')}
            className="p-2 rounded-2xl bg-[#1E1E1E] border border-[#2D2D2D] text-zinc-400 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="font-bold text-base text-white">Settings & Permissions</h1>
            <span className="text-[10px] text-blue-400 font-mono uppercase">Cameraman AI v2.4</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-md mx-auto w-full p-6 space-y-6 flex-1">
        <div className="p-5 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] space-y-4">
          <div className="flex items-center gap-3 pb-4 border-b border-[#252525]">
            <div className="w-10 h-10 rounded-2xl bg-blue-600/20 text-blue-400 flex items-center justify-center">
              <Camera className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-white text-sm">CameraX Preview & Recording</h4>
              <p className="text-xs text-zinc-400">Hardware accelerated camera stream active.</p>
            </div>
          </div>

          <div className="flex items-center gap-3 pb-4 border-b border-[#252525]">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600/20 text-indigo-400 flex items-center justify-center">
              <Mic className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-white text-sm">Audio & Rhythm Analysis</h4>
              <p className="text-xs text-zinc-400">Local audio decoding and beat drop detection.</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-emerald-600/20 text-emerald-400 flex items-center justify-center">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-white text-sm">Room & Local Storage</h4>
              <p className="text-xs text-zinc-400">Projects and recorded clips stored securely locally.</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
