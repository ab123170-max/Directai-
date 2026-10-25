import React, { useMemo, useState } from 'react';
import { AppScreen, MusicVideoProject } from '../types';
import {
  ArrowLeft, Film, Play, Music, Download, CheckCircle2, SlidersHorizontal,
  Sparkles, Video, Scissors, Captions, Palette, Layers3, Ratio, ChevronDown,
  CircleHelp, WandSparkles, AudioLines, FileJson, MoveUpRight
} from 'lucide-react';

interface VideoEditingStudioScreenProps {
  activeProject: MusicVideoProject;
  onUpdateProject: (proj: MusicVideoProject) => void;
  onNavigate: (screen: AppScreen) => void;
}

const looks = [
  { id: 'clean', label: 'Clean', css: 'none' },
  { id: 'cinematic', label: 'Cinematic', css: 'contrast(1.12) saturate(.82) sepia(.12)' },
  { id: 'vivid', label: 'Vivid', css: 'saturate(1.35) contrast(1.05)' },
  { id: 'mono', label: 'Mono', css: 'grayscale(1) contrast(1.08)' },
  { id: 'cool', label: 'Cool', css: 'saturate(.9) hue-rotate(12deg) contrast(1.04)' },
];

export const VideoEditingStudioScreen: React.FC<VideoEditingStudioScreenProps> = ({
  activeProject, onNavigate,
}) => {
  const completedShots = useMemo(
    () => activeProject.shots.filter(s => s.status === 'COMPLETED' && s.recordedClipUrl),
    [activeProject.shots]
  );
  const [selectedClip, setSelectedClip] = useState<string | null>(completedShots[0]?.recordedClipUrl || null);
  const [aspect, setAspect] = useState('9:16');
  const [look, setLook] = useState('cinematic');
  const [transition, setTransition] = useState('Smooth dissolve');
  const [captionPlan, setCaptionPlan] = useState(false);
  const [showExportPlan, setShowExportPlan] = useState(false);
  const selectedLook = looks.find(item => item.id === look) || looks[0];
  const selectedShot = completedShots.find(shot => shot.recordedClipUrl === selectedClip);

  const downloadEditPlan = () => {
    const plan = {
      project: activeProject.title,
      format: aspect,
      visualLook: selectedLook.label,
      transition,
      captionsPlanned: captionPlan,
      soundtrack: activeProject.song.title,
      clips: completedShots.map((shot, index) => ({
        order: index + 1, shot: shot.shotNumber, name: shot.name,
        source: shot.recordedClipUrl, durationSeconds: shot.duration,
        angle: shot.cameraAngle, movement: shot.cameraMovement,
        transitionAfter: index < completedShots.length - 1 ? transition : 'None',
      })),
      note: 'Edit decision plan only. This file does not render or merge video clips.',
    };
    const blob = new Blob([JSON.stringify(plan, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'cameraman-ai-edit-plan.json';
    a.click();
    URL.revokeObjectURL(url);
    setShowExportPlan(true);
  };

  return (
    <div className="min-h-screen bg-[#09090d] text-zinc-100">
      <header className="sticky top-0 z-30 border-b border-white/[0.07] bg-[#09090d]/90 backdrop-blur-2xl">
        <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-3.5">
          <div className="flex items-center gap-3">
            <button onClick={() => onNavigate('home')} aria-label="Back" className="rounded-xl border border-white/[0.08] bg-white/[0.04] p-2.5 text-zinc-300"><ArrowLeft className="h-5 w-5" /></button>
            <div><h1 className="text-sm font-extrabold text-white">Edit Studio</h1><p className="mt-0.5 text-[9px] font-bold uppercase tracking-[0.18em] text-violet-300">Creator timeline · 2026</p></div>
          </div>
          <button onClick={downloadEditPlan} disabled={!completedShots.length} className="flex items-center gap-2 rounded-xl bg-violet-500 px-3.5 py-2.5 text-xs font-extrabold text-white transition active:scale-95 disabled:opacity-40"><Download className="h-4 w-4" /> Export plan</button>
        </div>
      </header>

      <main className="mx-auto max-w-2xl space-y-5 px-4 pb-10 pt-5">
        <section className="flex items-center justify-between">
          <div><p className="text-[10px] font-bold uppercase tracking-[0.2em] text-zinc-500">Current project</p><h2 className="mt-1 text-xl font-extrabold tracking-tight text-white">{activeProject.title}</h2><p className="mt-1 text-xs text-zinc-500">{completedShots.length} captured clips · {activeProject.targetDuration}s target</p></div>
          <span className="rounded-full border border-emerald-400/20 bg-emerald-400/[0.08] px-2.5 py-1.5 text-[9px] font-bold text-emerald-300">{completedShots.length ? 'MEDIA READY' : 'WAITING FOR CLIPS'}</span>
        </section>

        <section className="overflow-hidden rounded-[26px] border border-white/[0.08] bg-[#111117]">
          <div className="flex items-center justify-between border-b border-white/[0.06] px-4 py-3">
            <div className="flex items-center gap-2"><Film className="h-4 w-4 text-violet-300" /><span className="text-xs font-bold text-zinc-200">Preview monitor</span></div>
            <span className="font-mono text-[9px] text-zinc-500">{aspect} · {selectedLook.label.toUpperCase()}</span>
          </div>
          <div className="flex justify-center bg-[#08080b] px-4 py-5">
            <div className={`relative overflow-hidden rounded-2xl border border-white/10 bg-[#15151d] ${aspect === '9:16' ? 'aspect-[9/16] w-[min(62vw,260px)]' : aspect === '1:1' ? 'aspect-square w-[min(70vw,320px)]' : 'aspect-video w-full max-w-lg'}`}>
              {selectedClip ? (
                <video key={selectedClip} src={selectedClip} controls playsInline className="h-full w-full object-contain" style={{ filter: selectedLook.css }} />
              ) : (
                <div className="flex h-full min-h-48 flex-col items-center justify-center px-6 text-center">
                  <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-violet-500/10 text-violet-300"><Video className="h-7 w-7" /></div>
                  <p className="text-sm font-bold text-zinc-300">Your edit starts here</p><p className="mt-2 text-[11px] leading-5 text-zinc-500">Capture your planned shots first. Completed clips will appear in this timeline.</p>
                  <button onClick={() => onNavigate('camera')} className="mt-4 rounded-xl bg-violet-500 px-4 py-2.5 text-xs font-bold text-white">Open camera director</button>
                </div>
              )}
              {selectedClip && <div className="pointer-events-none absolute left-3 top-3 rounded-lg border border-white/10 bg-black/55 px-2 py-1 text-[8px] font-bold uppercase tracking-widest text-white/80">Preview · {selectedShot?.name || 'Clip'}</div>}
            </div>
          </div>
          <div className="grid grid-cols-3 divide-x divide-white/[0.06] border-t border-white/[0.06]">
            <div className="p-3 text-center"><p className="text-lg font-extrabold text-white">{completedShots.length}</p><p className="text-[9px] uppercase tracking-wider text-zinc-500">Clips</p></div>
            <div className="p-3 text-center"><p className="text-lg font-extrabold text-white">{completedShots.reduce((sum, s) => sum + (s.duration || 0), 0)}s</p><p className="text-[9px] uppercase tracking-wider text-zinc-500">Planned length</p></div>
            <div className="p-3 text-center"><p className="text-lg font-extrabold text-white">{captionPlan ? 'On' : 'Off'}</p><p className="text-[9px] uppercase tracking-wider text-zinc-500">Caption plan</p></div>
          </div>
        </section>

        <section className="space-y-3">
          <div className="flex items-center justify-between"><h3 className="text-sm font-extrabold text-white">Timeline</h3><span className="text-[10px] text-zinc-500">Tap a clip to preview</span></div>
          {completedShots.length ? (
            <div className="flex gap-3 overflow-x-auto pb-2">
              {completedShots.map((shot, index) => {
                const selected = selectedClip === shot.recordedClipUrl;
                return <button key={shot.id} onClick={() => setSelectedClip(shot.recordedClipUrl || null)} className={`w-[108px] shrink-0 rounded-2xl border p-2 text-left transition ${selected ? 'border-violet-400 bg-violet-500/10' : 'border-white/[0.07] bg-[#111117]'}`}>
                  <div className="relative mb-2 aspect-[9/14] overflow-hidden rounded-xl bg-black">
                    <video src={shot.recordedClipUrl} muted playsInline className="h-full w-full object-cover" style={{ filter: selectedLook.css }} />
                    <span className="absolute left-1.5 top-1.5 rounded bg-black/70 px-1.5 py-1 font-mono text-[8px] text-white">{String(index + 1).padStart(2, '0')}</span>
                    {selected && <span className="absolute bottom-1.5 right-1.5 rounded-full bg-violet-500 p-1"><Play className="h-3 w-3 fill-white text-white" /></span>}
                  </div>
                  <p className="truncate text-[10px] font-bold text-zinc-200">{shot.name}</p><p className="mt-1 text-[9px] text-zinc-500">{shot.duration}s · {shot.cameraAngle}</p>
                </button>;
              })}
            </div>
          ) : <div className="rounded-2xl border border-dashed border-white/10 bg-white/[0.02] p-5 text-center"><Layers3 className="mx-auto h-6 w-6 text-zinc-600" /><p className="mt-2 text-xs font-semibold text-zinc-400">No recorded clips yet</p><button onClick={() => onNavigate('shotDirector')} className="mt-3 text-xs font-bold text-violet-300">Review shot plan →</button></div>}
        </section>

        <section className="space-y-3">
          <h3 className="text-sm font-extrabold text-white">Quick adjustments</h3>
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl border border-white/[0.07] bg-[#111117] p-4">
              <div className="mb-3 flex items-center gap-2 text-violet-300"><Ratio className="h-4 w-4" /><span className="text-xs font-bold text-zinc-200">Canvas ratio</span></div>
              <div className="grid grid-cols-3 gap-1.5">{['9:16','1:1','16:9'].map(r => <button key={r} onClick={() => setAspect(r)} className={`rounded-lg py-2 text-[10px] font-bold ${aspect === r ? 'bg-violet-500 text-white' : 'bg-white/[0.05] text-zinc-500'}`}>{r}</button>)}</div>
            </div>
            <div className="rounded-2xl border border-white/[0.07] bg-[#111117] p-4">
              <div className="mb-3 flex items-center gap-2 text-cyan-300"><Captions className="h-4 w-4" /><span className="text-xs font-bold text-zinc-200">Captions</span></div>
              <button onClick={() => setCaptionPlan(v => !v)} className={`flex w-full items-center justify-between rounded-lg px-3 py-2 text-[10px] font-bold ${captionPlan ? 'bg-cyan-400/15 text-cyan-200' : 'bg-white/[0.05] text-zinc-500'}`}><span>{captionPlan ? 'Included in plan' : 'Add to plan'}</span><span>{captionPlan ? 'ON' : 'OFF'}</span></button>
              <p className="mt-2 text-[9px] leading-4 text-zinc-600">Plans captions only; speech-to-text is not connected yet.</p>
            </div>
          </div>

          <div className="rounded-2xl border border-white/[0.07] bg-[#111117] p-4">
            <div className="mb-3 flex items-center gap-2"><Palette className="h-4 w-4 text-amber-300" /><span className="text-xs font-bold text-zinc-200">Preview look</span><span className="ml-auto text-[9px] text-zinc-600">Preview only</span></div>
            <div className="grid grid-cols-5 gap-2">{looks.map(item => <button key={item.id} onClick={() => setLook(item.id)} className={`rounded-xl border p-2 text-center transition ${look === item.id ? 'border-violet-400 bg-violet-500/10' : 'border-white/[0.06] bg-white/[0.02]'}`}><span className="mx-auto mb-1.5 block h-6 w-6 rounded-lg bg-gradient-to-br from-amber-200 via-rose-400 to-indigo-600" style={{ filter: item.css }} /><span className="text-[9px] font-semibold text-zinc-400">{item.label}</span></button>)}</div>
          </div>

          <div className="rounded-2xl border border-white/[0.07] bg-[#111117] p-4">
            <div className="mb-3 flex items-center gap-2"><SlidersHorizontal className="h-4 w-4 text-violet-300" /><span className="text-xs font-bold text-zinc-200">Transition plan</span></div>
            <div className="relative"><select value={transition} onChange={e => setTransition(e.target.value)} className="w-full appearance-none rounded-xl border border-white/[0.08] bg-[#0b0b10] px-3 py-3 text-xs font-semibold text-zinc-200 outline-none focus:border-violet-400"><option>Smooth dissolve</option><option>Hard cut</option><option>Beat cut</option><option>Fade to black</option><option>Whip-pan concept</option></select><ChevronDown className="pointer-events-none absolute right-3 top-3 h-4 w-4 text-zinc-500" /></div>
            <p className="mt-2 flex items-start gap-1.5 text-[9px] leading-4 text-zinc-600"><CircleHelp className="mt-0.5 h-3 w-3 shrink-0" />This selects a transition in the edit plan. It does not apply a rendered transition to the video yet.</p>
          </div>
        </section>

        <section className="rounded-[24px] border border-violet-300/15 bg-gradient-to-br from-violet-500/[0.12] to-cyan-400/[0.04] p-4">
          <div className="flex items-start gap-3"><div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-violet-400/10 text-violet-300"><WandSparkles className="h-5 w-5" /></div><div className="flex-1"><h3 className="text-sm font-extrabold text-white">AI edit preparation</h3><p className="mt-1 text-[11px] leading-5 text-zinc-400">Create a structured plan with your clip order, canvas ratio, visual look and transition choices. Actual MP4 rendering, clip merging, beat analysis and auto-captions still need an export engine.</p></div></div>
          <button onClick={downloadEditPlan} disabled={!completedShots.length} className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-white/[0.08] py-3 text-xs font-bold text-zinc-100 transition hover:bg-white/[0.12] disabled:opacity-40"><FileJson className="h-4 w-4" /> Download editable plan (JSON) <MoveUpRight className="h-3.5 w-3.5" /></button>
          {showExportPlan && <div className="mt-3 flex items-start gap-2 rounded-xl border border-emerald-400/20 bg-emerald-400/[0.06] p-3 text-[10px] leading-4 text-emerald-200"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />Edit plan downloaded. This is a project plan, not a rendered video file.</div>}
        </section>

        <div className="flex items-center gap-3 rounded-2xl border border-white/[0.06] bg-[#101016] p-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-500/10 text-violet-300"><Music className="h-5 w-5" /></div>
          <div className="min-w-0 flex-1"><p className="truncate text-xs font-bold text-zinc-200">{activeProject.song.title}</p><p className="mt-1 text-[10px] text-zinc-500">{activeProject.song.artist} · {activeProject.song.bpm} BPM</p></div>
          <AudioLines className="h-5 w-5 text-zinc-500" />
        </div>
      </main>
    </div>
  );
};
