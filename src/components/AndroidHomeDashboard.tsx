import React from 'react';
import { AppScreen, MusicVideoProject } from '../types';
import {
  Video, Music, Plus, Sparkles, FolderOpen, Settings, ChevronRight,
  Clapperboard, WandSparkles, AudioLines, Scissors, Captions, Palette,
  Layers3, Camera, Zap, CirclePlay, ArrowUpRight, Film, Clock3
} from 'lucide-react';

interface AndroidHomeDashboardProps {
  projects: MusicVideoProject[];
  activeProject: MusicVideoProject;
  onSelectProject: (proj: MusicVideoProject) => void;
  onCreateNew: () => void;
  onNavigate: (screen: AppScreen) => void;
}

const quickTools = [
  { title: 'AI Shot Planner', detail: 'Build a scene-by-scene plan', icon: Clapperboard, screen: 'songStudio' as AppScreen, tag: 'DIRECT' },
  { title: 'Smart Editor', detail: 'Arrange and preview clips', icon: Scissors, screen: 'editing' as AppScreen, tag: 'EDIT' },
  { title: 'Camera Director', detail: 'Shoot with movement cues', icon: Camera, screen: 'camera' as AppScreen, tag: 'SHOOT' },
  { title: 'Beat Studio', detail: 'Plan cuts around your track', icon: AudioLines, screen: 'songStudio' as AppScreen, tag: 'AUDIO' },
  { title: 'Caption & Story', detail: 'Plan titles and hook moments', icon: Captions, screen: 'shotDirector' as AppScreen, tag: 'STORY' },
  { title: 'Look & Color', detail: 'Choose a visual mood', icon: Palette, screen: 'editing' as AppScreen, tag: 'STYLE' },
];

export const AndroidHomeDashboard: React.FC<AndroidHomeDashboardProps> = ({
  projects, activeProject, onSelectProject, onCreateNew, onNavigate,
}) => {
  const completedShots = activeProject?.shots?.filter(s => s.status === 'COMPLETED').length || 0;
  const totalShots = activeProject?.shots?.length || 0;
  const progress = totalShots ? Math.round((completedShots / totalShots) * 100) : 0;

  return (
    <div className="min-h-screen bg-[#09090d] text-zinc-100 flex flex-col select-none overflow-x-hidden">
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-28 h-96 w-96 rounded-full bg-violet-600/10 blur-3xl" />
        <div className="absolute top-[34rem] -left-32 h-80 w-80 rounded-full bg-cyan-500/[0.06] blur-3xl" />
      </div>

      <header className="sticky top-0 z-30 border-b border-white/[0.07] bg-[#09090d]/85 backdrop-blur-2xl">
        <div className="mx-auto flex max-w-2xl items-center justify-between px-5 py-4">
          <div className="flex items-center gap-3">
            <div className="relative flex h-11 w-11 items-center justify-center rounded-[17px] bg-gradient-to-br from-violet-500 to-indigo-700 shadow-lg shadow-violet-900/40">
              <Video className="h-5 w-5 text-white" />
              <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full border-2 border-[#09090d] bg-emerald-400" />
            </div>
            <div>
              <h1 className="text-[17px] font-extrabold tracking-tight text-white">Cameraman <span className="text-violet-300">AI</span></h1>
              <p className="mt-0.5 text-[9px] font-bold uppercase tracking-[0.2em] text-zinc-500">Creator Studio · 2026</p>
            </div>
          </div>
          <button onClick={() => onNavigate('settings')} aria-label="Settings" className="rounded-2xl border border-white/[0.08] bg-white/[0.04] p-3 text-zinc-300 transition hover:bg-white/[0.09]">
            <Settings className="h-5 w-5" />
          </button>
        </div>
      </header>

      <main className="relative z-10 mx-auto w-full max-w-2xl flex-1 space-y-8 px-5 pb-10 pt-6">
        <section>
          <div className="mb-4 flex items-end justify-between">
            <div>
              <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.24em] text-violet-300">Your creative co-pilot</p>
              <h2 className="text-3xl font-extrabold leading-tight tracking-[-0.04em] text-white">Imagine it.<br /><span className="text-zinc-500">Shoot it. Edit it.</span></h2>
            </div>
            <div className="mb-1 flex items-center gap-1 rounded-full border border-emerald-400/20 bg-emerald-400/[0.08] px-2.5 py-1.5 text-[9px] font-bold uppercase tracking-wider text-emerald-300">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" /> Studio ready
            </div>
          </div>

          <div className="relative overflow-hidden rounded-[30px] border border-violet-300/20 bg-gradient-to-br from-[#241748] via-[#17152c] to-[#10131f] p-5 shadow-2xl shadow-violet-950/30 sm:p-6">
            <div className="pointer-events-none absolute -right-8 -top-10 h-48 w-48 rounded-full border border-white/[0.07]" />
            <div className="pointer-events-none absolute -right-1 top-0 h-36 w-36 rounded-full bg-violet-500/20 blur-3xl" />
            <div className="relative">
              <div className="mb-5 flex items-center justify-between">
                <span className="inline-flex items-center gap-2 rounded-full border border-violet-200/20 bg-white/[0.07] px-3 py-1.5 text-[10px] font-bold text-violet-100">
                  <WandSparkles className="h-3.5 w-3.5" /> AI VIDEO WORKFLOW
                </span>
                <Sparkles className="h-5 w-5 text-violet-200" />
              </div>
              <h3 className="max-w-xs text-2xl font-extrabold leading-tight tracking-tight text-white">From first idea to final cut.</h3>
              <p className="mt-2 max-w-sm text-xs leading-6 text-zinc-300">Plan the hook, map your shots, follow on-screen camera cues, then bring your clips into the edit studio.</p>
              <button onClick={onCreateNew} className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-white px-5 py-4 text-sm font-extrabold text-[#17122b] shadow-xl transition active:scale-[0.98] sm:w-auto sm:min-w-56">
                <Plus className="h-5 w-5" /> Start a new video <ChevronRight className="ml-1 h-4 w-4" />
              </button>
            </div>
            <div className="relative mt-6 grid grid-cols-3 gap-2 border-t border-white/[0.09] pt-4">
              {[['01','Plan'],['02','Shoot'],['03','Edit']].map(([n,label]) => (
                <div key={n} className="flex items-center gap-2">
                  <span className="font-mono text-xs text-violet-300">{n}</span>
                  <span className="text-[11px] font-semibold text-zinc-300">{label}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section>
          <div className="mb-3 flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-white">Creative tools</h3>
              <p className="mt-1 text-[11px] text-zinc-500">Pick the next step in your workflow</p>
            </div>
            <span className="rounded-full border border-white/[0.08] px-2.5 py-1 text-[9px] font-bold uppercase tracking-widest text-zinc-500">6 tools</span>
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {quickTools.map((tool, i) => {
              const Icon = tool.icon;
              return (
                <button key={tool.title} onClick={() => onNavigate(tool.screen)} className="group min-h-[138px] rounded-[24px] border border-white/[0.07] bg-[#13131a]/90 p-4 text-left transition hover:border-violet-300/30 hover:bg-[#1a1925] active:scale-[0.98]">
                  <div className="mb-4 flex items-start justify-between">
                    <span className={`flex h-10 w-10 items-center justify-center rounded-2xl ${i % 3 === 0 ? 'bg-violet-500/15 text-violet-300' : i % 3 === 1 ? 'bg-cyan-400/10 text-cyan-300' : 'bg-amber-400/10 text-amber-300'}`}>
                      <Icon className="h-5 w-5" />
                    </span>
                    <ArrowUpRight className="h-4 w-4 text-zinc-600 transition group-hover:text-violet-300" />
                  </div>
                  <h4 className="text-[13px] font-bold text-zinc-100">{tool.title}</h4>
                  <p className="mt-1.5 text-[10px] leading-4 text-zinc-500">{tool.detail}</p>
                </button>
              );
            })}
          </div>
        </section>

        {activeProject && (
          <section>
            <div className="mb-3 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white">Continue creating</h3>
                <p className="mt-1 text-[11px] text-zinc-500">Pick up where you left off</p>
              </div>
              <button onClick={() => onNavigate('home')} className="text-[11px] font-semibold text-violet-300">All projects</button>
            </div>
            <button onClick={() => onNavigate('shotDirector')} className="w-full rounded-[26px] border border-white/[0.08] bg-[#13131a] p-4 text-left transition hover:border-violet-300/25 sm:p-5">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-500/20 to-indigo-500/10 text-violet-300">
                  <Film className="h-6 w-6" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="mb-1 flex items-center gap-2">
                    <span className="rounded-full bg-violet-500/10 px-2 py-0.5 text-[8px] font-bold uppercase tracking-widest text-violet-300">{activeProject.shootingStyle}</span>
                    <span className="text-[9px] text-zinc-600"><Clock3 className="mr-1 inline h-3 w-3" />{activeProject.targetDuration}s</span>
                  </div>
                  <h4 className="truncate text-sm font-bold text-white">{activeProject.title}</h4>
                  <p className="mt-1 truncate text-[11px] text-zinc-500">{activeProject.song.title} · {completedShots}/{totalShots} shots captured</p>
                </div>
                <ChevronRight className="h-5 w-5 shrink-0 text-zinc-500" />
              </div>
              <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-white/[0.06]">
                <div className="h-full rounded-full bg-gradient-to-r from-violet-500 to-cyan-300 transition-all" style={{ width: `${progress}%` }} />
              </div>
              <div className="mt-2 flex justify-between text-[9px] font-semibold text-zinc-500"><span>PROJECT PROGRESS</span><span>{progress}%</span></div>
            </button>
          </section>
        )}

        <section>
          <div className="mb-3 flex items-center justify-between">
            <div>
              <h3 className="text-base font-bold text-white">Recent projects</h3>
              <p className="mt-1 text-[11px] text-zinc-500">Saved on this device</p>
            </div>
            <FolderOpen className="h-4 w-4 text-zinc-500" />
          </div>
          <div className="space-y-2">
            {projects.map((proj) => (
              <button key={proj.id} onClick={() => onSelectProject(proj)} className="flex w-full items-center gap-3 rounded-2xl border border-white/[0.06] bg-[#111117] p-3 text-left transition hover:bg-white/[0.04]">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/[0.05] text-zinc-400"><Music className="h-4 w-4" /></div>
                <div className="min-w-0 flex-1">
                  <h4 className="truncate text-xs font-bold text-zinc-200">{proj.title}</h4>
                  <p className="mt-1 truncate text-[10px] text-zinc-500">{proj.song.title} · {proj.shots.length} planned shots</p>
                </div>
                <ChevronRight className="h-4 w-4 text-zinc-600" />
              </button>
            ))}
          </div>
        </section>

        <button onClick={() => onNavigate('editing')} className="flex w-full items-center gap-3 rounded-2xl border border-white/[0.07] bg-gradient-to-r from-[#171720] to-[#111117] p-4 text-left transition hover:border-cyan-300/20">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-300/10 text-cyan-300"><Layers3 className="h-5 w-5" /></div>
          <div className="flex-1"><p className="text-xs font-bold text-zinc-200">Open editing timeline</p><p className="mt-1 text-[10px] text-zinc-500">Preview captured clips and prepare your sequence</p></div>
          <CirclePlay className="h-5 w-5 text-zinc-500" />
        </button>

        <p className="pb-2 text-center text-[9px] leading-5 text-zinc-600">Cameraman AI · Plan smarter, shoot with purpose, edit with control.<br />AI suggestions support creativity; final results depend on your footage and choices.</p>
      </main>
    </div>
  );
};
