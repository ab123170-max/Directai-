import React, { useRef, useState } from 'react';
import { AppScreen } from '../types';
import { Heart, MessageCircle, Bookmark, Share2, Plus, Home, Search, UserRound, Clapperboard, Volume2, VolumeX, Sparkles, Play, Camera } from 'lucide-react';

interface Props { onNavigate: (screen: AppScreen) => void; }

const clips = [
  { user: 'cinematic.creator', title: 'Golden hour • slow push-in', sound: 'Original audio · Cinematic mood', tag: '#cinematic #filmmaking', video: 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4', tint: 'from-orange-950/40 via-transparent to-black/80' },
  { user: 'travel.frames', title: 'Make a simple place look epic ✨', sound: 'Dreamy instrumental · 92 BPM', tag: '#travelvideo #shotideas', video: 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4', tint: 'from-blue-950/30 via-transparent to-black/80' },
  { user: 'director.ai', title: 'Try this low-angle hero shot', sound: 'AI Director · Shot recipe', tag: '#cameratips #reels', video: 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4', tint: 'from-violet-950/40 via-transparent to-black/80' },
];

export const TikTokFeedScreen: React.FC<Props> = ({ onNavigate }) => {
  const [tab, setTab] = useState('For You');
  const [liked, setLiked] = useState<number[]>([]);
  const [saved, setSaved] = useState<number[]>([]);
  const [following, setFollowing] = useState<string[]>([]);
  const [muted, setMuted] = useState(true);
  const [active, setActive] = useState(0);
  const [comments, setComments] = useState<number | null>(null);
  const [message, setMessage] = useState('');
  const feedRef = useRef<HTMLDivElement>(null);

  const toggle = (list: number[], setter: (v: number[]) => void, i: number) =>
    setter(list.includes(i) ? list.filter(n => n !== i) : [...list, i]);

  return (
    <div className="fixed inset-0 z-40 bg-black text-white">
      <div ref={feedRef} className="h-[100dvh] overflow-y-auto snap-y snap-mandatory overscroll-contain" onScroll={(e) => {
        const el = e.currentTarget;
        setActive(Math.max(0, Math.min(clips.length - 1, Math.round(el.scrollTop / el.clientHeight))));
      }}>
        {clips.map((clip, i) => (
          <section key={clip.user} className="relative h-[100dvh] w-full snap-start snap-always overflow-hidden bg-[#141414]">
            <video className="absolute inset-0 h-full w-full object-cover" src={clip.video} autoPlay muted={muted} loop playsInline preload={i === 0 ? 'auto' : 'metadata'} onClick={() => setMuted(!muted)} />
            <div className={`absolute inset-0 bg-gradient-to-b ${clip.tint} pointer-events-none`} />
            <div className="absolute left-0 right-0 top-0 z-10 flex items-center justify-center gap-5 px-4 pt-[max(18px,env(safe-area-inset-top))]">
              <button onClick={() => setTab('Following')} className={`text-sm font-semibold ${tab === 'Following' ? 'text-white' : 'text-white/60'}`}>Following</button>
              <span className="h-4 w-px bg-white/25" />
              <button onClick={() => setTab('For You')} className={`relative text-sm font-extrabold ${tab === 'For You' ? 'text-white' : 'text-white/60'}`}>For You{tab === 'For You' && <span className="absolute -bottom-2 left-1/2 h-0.5 w-7 -translate-x-1/2 rounded-full bg-white" />}</button>
              <button onClick={() => onNavigate('shotDirector')} className="absolute right-4 rounded-full bg-black/30 p-2"><Sparkles size={19} /></button>
            </div>

            <div className="absolute bottom-[calc(74px+env(safe-area-inset-bottom))] left-4 right-[78px] z-10">
              <div className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-black/25 px-3 py-1.5 text-[10px] font-bold backdrop-blur-md"><Sparkles size={12} className="text-cyan-300" /> CAMERAMAN AI • CREATOR FEED</div>
              <button onClick={() => setFollowing(prev => prev.includes(clip.user) ? prev.filter(u => u !== clip.user) : [...prev, clip.user])} className="mb-2 flex items-center gap-2 text-sm font-extrabold">@{clip.user} <span className="rounded-full bg-white px-2 py-1 text-[9px] font-black text-black">{following.includes(clip.user) ? 'Following' : '+ Follow'}</span></button>
              <p className="text-sm font-semibold leading-5">{clip.title}</p>
              <p className="mt-1.5 text-xs text-white/80">{clip.tag}</p>
              <div className="mt-3 flex items-center gap-2 text-xs text-white/85"><span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-white/15"><Play size={10} fill="white" /></span><span className="truncate">{clip.sound}</span></div>
            </div>

            <div className="absolute bottom-[calc(82px+env(safe-area-inset-bottom))] right-3 z-10 flex w-14 flex-col items-center gap-5">
              <div className="relative mb-1">
                <div className="flex h-12 w-12 items-center justify-center rounded-full border-2 border-white bg-gradient-to-br from-fuchsia-500 to-cyan-400 text-sm font-black">AI</div>
                <button onClick={() => onNavigate('camera')} className="absolute -bottom-2 left-1/2 flex h-5 w-5 -translate-x-1/2 items-center justify-center rounded-full bg-pink-500"><Plus size={14} /></button>
              </div>
              <button onClick={() => toggle(liked,setLiked,i)} className="flex flex-col items-center gap-1"><Heart size={29} fill={liked.includes(i) ? '#ff2d55' : 'white'} className={liked.includes(i) ? 'text-[#ff2d55]' : 'text-white'} /><span className="text-[10px] font-bold">{liked.includes(i) ? '1.2K+' : '1.2K'}</span></button>
              <button onClick={() => setComments(i)} className="flex flex-col items-center gap-1"><MessageCircle size={28} fill="white" className="text-white" /><span className="text-[10px] font-bold">86</span></button>
              <button onClick={() => toggle(saved,setSaved,i)} className="flex flex-col items-center gap-1"><Bookmark size={27} fill={saved.includes(i) ? '#facc15' : 'white'} className={saved.includes(i) ? 'text-yellow-300' : 'text-white'} /><span className="text-[10px] font-bold">Save</span></button>
              <button onClick={() => { if (navigator.share) navigator.share({title: clip.title, text: 'Check out this Cameraman AI shot idea'}).catch(()=>{}); else setMessage('Share is ready — copy this video idea from Cameraman AI.'); }} className="flex flex-col items-center gap-1"><Share2 size={26} /><span className="text-[10px] font-bold">Share</span></button>
              <button onClick={() => setMuted(!muted)} aria-label="Toggle sound" className="mt-1 rounded-full border border-white/20 bg-black/30 p-2">{muted ? <VolumeX size={18}/> : <Volume2 size={18}/>}</button>
            </div>
            <div className="absolute bottom-[calc(62px+env(safe-area-inset-bottom))] left-4 right-20 z-10 h-0.5 bg-white/30"><div className="h-full bg-white/80" style={{width: active === i ? '45%' : '0%'}} /></div>
          </section>
        ))}
      </div>

      <nav className="absolute bottom-0 left-0 right-0 z-20 flex h-[calc(62px+env(safe-area-inset-bottom))] items-start justify-around border-t border-white/10 bg-black/95 px-2 pt-2 pb-[env(safe-area-inset-bottom)]">
        <button onClick={() => { feedRef.current?.scrollTo({top:0,behavior:'smooth'}); setTab('For You'); }} className="flex w-14 flex-col items-center gap-1 text-white"><Home size={22} fill="white" /><span className="text-[9px] font-bold">Home</span></button>
        <button onClick={() => onNavigate('home')} className="flex w-14 flex-col items-center gap-1 text-white/60"><Search size={22}/><span className="text-[9px]">Discover</span></button>
        <button onClick={() => onNavigate('camera')} className="flex h-8 w-12 items-center justify-center rounded-lg bg-gradient-to-r from-cyan-400 via-white to-pink-500"><span className="flex h-7 w-10 items-center justify-center rounded-md bg-black text-white"><Plus size={24}/></span></button>
        <button onClick={() => onNavigate('shotDirector')} className="flex w-14 flex-col items-center gap-1 text-white/60"><Clapperboard size={22}/><span className="text-[9px]">AI Director</span></button>
        <button onClick={() => onNavigate('settings')} className="flex w-14 flex-col items-center gap-1 text-white/60"><UserRound size={22}/><span className="text-[9px]">Profile</span></button>
      </nav>

      {comments !== null && <div className="absolute inset-0 z-50 flex items-end bg-black/50" onClick={() => setComments(null)}><div className="w-full rounded-t-3xl bg-[#171717] p-5 pb-8" onClick={e=>e.stopPropagation()}><div className="mx-auto mb-4 h-1 w-10 rounded-full bg-white/30"/><div className="mb-4 flex items-center justify-between"><h3 className="font-bold">Comments · 86</h3><button onClick={() => setComments(null)}>✕</button></div><p className="mb-4 text-sm text-white/70">Love this shot idea! 🔥 Try it with a slow push-in and backlight.</p><form onSubmit={e=>{e.preventDefault();setMessage('Comment added locally.');setComments(null);}} className="flex gap-2"><input value={message} onChange={e=>setMessage(e.target.value)} placeholder="Add a comment..." className="min-w-0 flex-1 rounded-full bg-white/10 px-4 py-3 text-sm outline-none"/><button className="rounded-full bg-pink-500 px-4 text-sm font-bold">Post</button></form></div></div>}
      {message && comments === null && <button onClick={()=>setMessage('')} className="absolute left-4 right-4 top-20 z-50 rounded-xl bg-zinc-800 p-3 text-xs">{message} · Tap to dismiss</button>}
    </div>
  );
};
