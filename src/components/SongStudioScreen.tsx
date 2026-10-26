import React, { useState } from 'react';
import { AppScreen, SongTrack, MusicVideoProject, ShootingStyle } from '../types';
import { SAMPLE_SONGS, analyzeAudioFile } from '../services/audioService';
import { Music, Upload, Play, Sparkles, ArrowLeft, ChevronRight, Sliders, Clock } from 'lucide-react';
import { generateAIShotListLive } from '../services/aiDirectorService';

interface SongStudioScreenProps {
  activeProject: MusicVideoProject;
  onUpdateProject: (proj: MusicVideoProject) => void;
  onNavigate: (screen: AppScreen) => void;
}

export const SongStudioScreen: React.FC<SongStudioScreenProps> = ({
  activeProject,
  onUpdateProject,
  onNavigate,
}) => {
  const [selectedSong, setSelectedSong] = useState<SongTrack>(activeProject.song || SAMPLE_SONGS[0]);
  const [selectedStyle, setSelectedStyle] = useState<ShootingStyle>(activeProject.shootingStyle || 'Hero and attitude');
  const [targetDuration, setTargetDuration] = useState<number>(activeProject.targetDuration || 30);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isGeneratingPlan, setIsGeneratingPlan] = useState(false);
  const [planError, setPlanError] = useState('');

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsAnalyzing(true);
    try {
      const analyzed = await analyzeAudioFile(file);
      setSelectedSong(analyzed);
    } catch (error) {
      setPlanError(error instanceof Error ? error.message : 'Could not analyze this audio file.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleProceedToDirector = async () => {
    setIsGeneratingPlan(true);
    setPlanError('');
    try {
    const shots = await generateAIShotListLive(selectedSong, selectedStyle, targetDuration);
    onUpdateProject({
      ...activeProject,
      song: selectedSong,
      shootingStyle: selectedStyle,
      targetDuration,
      shots,
      updatedAt: Date.now(),
    });
    onNavigate('shotDirector');
    } catch (error) {
      setPlanError(error instanceof Error ? error.message : 'Gemini shot-plan generation failed.');
    } finally {
      setIsGeneratingPlan(false);
    }
  };

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
            <h1 className="font-bold text-base text-white">Song & Rhythm Studio</h1>
            <span className="text-[10px] text-blue-400 font-mono uppercase">Phase 1: Audio Analysis</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-md mx-auto w-full p-6 space-y-6 flex-1">
        {/* Upload Audio Card */}
        <div className="p-5 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Audio Track Selection</h3>
            <label className="flex items-center gap-1.5 px-3.5 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-2xl cursor-pointer shadow-lg shadow-blue-600/30 transition-all">
              <Upload className="w-3.5 h-3.5" />
              <span>Import Audio</span>
              <input type="file" accept="audio/*" onChange={handleFileUpload} className="hidden" />
            </label>
          </div>

          {/* Sample Song List */}
          <div className="space-y-2">
            {SAMPLE_SONGS.map((song) => {
              const isSelected = selectedSong.id === song.id;
              return (
                <div
                  key={song.id}
                  onClick={() => setSelectedSong(song)}
                  className={`p-4 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                    isSelected ? 'bg-blue-600/10 border-blue-500 text-white' : 'bg-[#181818] border-[#252525] text-zinc-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${isSelected ? 'bg-blue-600 text-white' : 'bg-[#222222] text-zinc-400'}`}>
                      <Music className="w-4 h-4" />
                    </div>
                    <div>
                      <h4 className="font-bold text-sm text-white">{song.title}</h4>
                      <p className="text-[11px] text-zinc-400">{song.artist} • {song.bpm} BPM</p>
                    </div>
                  </div>
                  {isSelected && <span className="text-xs text-blue-400 font-bold">Selected</span>}
                </div>
              );
            })}
          </div>
        </div>

        {/* Waveform & Beat Analyzer Display */}
        <div className="p-5 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Waveform & Beat Markers</h3>
            <span className="text-xs font-mono text-blue-400">{selectedSong.bpm} BPM</span>
          </div>

          <div className="h-20 bg-[#141414] rounded-2xl border border-[#252525] p-3 flex items-end gap-1 overflow-hidden">
            {selectedSong.waveformData.map((val, idx) => (
              <div
                key={idx}
                className="flex-1 bg-blue-500/60 hover:bg-blue-400 rounded-full transition-all"
                style={{ height: `${val * 100}%` }}
              />
            ))}
          </div>

          <div className="flex items-center justify-between text-xs text-zinc-400 pt-2 border-t border-[#252525]">
            <span>Duration: {selectedSong.duration}s</span>
            <span>{selectedSong.beatMarkers.length} Beat Drops Detected</span>
          </div>
        </div>

        {/* Shooting Style & Video Duration Picker */}
        <div className="p-5 rounded-3xl bg-[#1A1A1A] border border-[#2A2A2A] space-y-4">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Cinematic Style</h3>
          
          <select
            value={selectedStyle}
            onChange={(e) => setSelectedStyle(e.target.value as ShootingStyle)}
            className="w-full bg-[#141414] border border-[#252525] rounded-2xl px-4 py-3 text-sm text-white font-medium focus:outline-none focus:border-blue-500"
          >
            <option value="Hero and attitude">Hero and attitude</option>
            <option value="Dance">Dance</option>
            <option value="Luxury cinematic">Luxury cinematic</option>
            <option value="Couple">Couple</option>
            <option value="Street cinematic">Street cinematic</option>
            <option value="Nature and travel">Nature and travel</option>
            <option value="Action-inspired">Action-inspired</option>
            <option value="Food and lifestyle">Food and lifestyle</option>
          </select>

          <div className="pt-2">
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-2">Target Duration ({targetDuration}s)</label>
            <div className="grid grid-cols-3 gap-2">
              {[15, 30, 60].map((dur) => (
                <button
                  key={dur}
                  onClick={() => setTargetDuration(dur)}
                  className={`py-2.5 rounded-xl text-xs font-bold transition-all border ${
                    targetDuration === dur ? 'bg-blue-600 border-blue-500 text-white' : 'bg-[#141414] border-[#252525] text-zinc-400'
                  }`}
                >
                  {dur} Seconds
                </button>
              ))}
            </div>
          </div>
        </div>

        {planError && <div role="alert" className="rounded-xl border border-red-500/40 bg-red-500/10 p-3 text-sm text-red-300">{planError}</div>}

        {/* Proceed Button */}
        <button
          onClick={handleProceedToDirector}
          disabled={isGeneratingPlan || isAnalyzing}
          className="w-full flex items-center justify-center gap-2 py-4 bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm rounded-2xl shadow-xl shadow-blue-600/30 transition-all active:scale-95"
        >
          <Sparkles className="w-4 h-4" />
          <span>{isGeneratingPlan ? 'GEMINI IS PLANNING…' : isAnalyzing ? 'ANALYZING AUDIO…' : 'GENERATE AI SHOT PLAN'}</span>
        </button>
      </main>
    </div>
  );
};
