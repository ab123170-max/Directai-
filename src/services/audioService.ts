import { SongTrack, BeatMarker } from '../types';

export const SAMPLE_SONGS: SongTrack[] = [
  {
    id: 'song-1',
    title: 'Neon Horizon',
    artist: 'Aura Sound',
    duration: 30,
    bpm: 124,
    waveformData: Array.from({ length: 50 }, () => Math.random() * 0.8 + 0.2),
    beatMarkers: [
      { id: 'b-1', timestamp: 0.0, intensity: 1.0, isBeatDrop: true },
      { id: 'b-2', timestamp: 4.8, intensity: 0.8, isBeatDrop: false },
      { id: 'b-3', timestamp: 9.6, intensity: 0.9, isBeatDrop: false },
      { id: 'b-4', timestamp: 14.5, intensity: 1.0, isBeatDrop: true },
      { id: 'b-5', timestamp: 19.3, intensity: 0.85, isBeatDrop: false },
      { id: 'b-6', timestamp: 24.1, intensity: 0.95, isBeatDrop: false },
      { id: 'b-7', timestamp: 28.9, intensity: 1.0, isBeatDrop: true },
    ],
  },
  {
    id: 'song-2',
    title: 'Midnight Drive',
    artist: 'Pulse Echo',
    duration: 30,
    bpm: 118,
    waveformData: Array.from({ length: 50 }, () => Math.random() * 0.7 + 0.3),
    beatMarkers: [
      { id: 'bm-1', timestamp: 0.0, intensity: 0.9, isBeatDrop: true },
      { id: 'bm-2', timestamp: 5.1, intensity: 0.8, isBeatDrop: false },
      { id: 'bm-3', timestamp: 10.2, intensity: 0.95, isBeatDrop: true },
      { id: 'bm-4', timestamp: 15.3, intensity: 0.85, isBeatDrop: false },
      { id: 'bm-5', timestamp: 20.4, intensity: 1.0, isBeatDrop: true },
      { id: 'bm-6', timestamp: 25.5, intensity: 0.9, isBeatDrop: false },
    ],
  },
];

export async function analyzeAudioFile(file: File): Promise<SongTrack> {
  const duration = 30; // Default preview song duration
  const bpm = Math.floor(Math.random() * 20) + 110;
  const waveformData = Array.from({ length: 50 }, () => Math.random() * 0.8 + 0.2);
  
  const beatMarkers: BeatMarker[] = [];
  const interval = 60 / bpm * 2; // Every 2 beats
  for (let t = 0; t < duration; t += interval) {
    beatMarkers.push({
      id: `beat-${Date.now()}-${t.toFixed(1)}`,
      timestamp: Number(t.toFixed(2)),
      intensity: Math.random() * 0.3 + 0.7,
      isBeatDrop: t === 0 || Math.abs(t - 15) < 0.5,
    });
  }

  return {
    id: `song-${Date.now()}`,
    title: file.name.replace(/\.[^/.]+$/, ''),
    artist: 'Local Artist',
    duration,
    bpm,
    audioUrl: URL.createObjectURL(file),
    waveformData,
    beatMarkers,
  };
}
