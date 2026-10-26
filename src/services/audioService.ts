import { SongTrack, BeatMarker } from '../types';

function buildWaveform(channel: Float32Array, bins = 50): number[] {
  const result: number[] = [];
  const binSize = Math.max(1, Math.floor(channel.length / bins));
  for (let i = 0; i < bins; i++) {
    const start = i * binSize;
    const end = Math.min(channel.length, start + binSize);
    let sum = 0;
    for (let j = start; j < end; j++) sum += channel[j] * channel[j];
    const rms = Math.sqrt(sum / Math.max(1, end - start));
    result.push(Math.max(0.04, Math.min(1, rms)));
  }
  const max = Math.max(...result, 0.001);
  return result.map(v => Number((v / max).toFixed(3)));
}

function analyzeBeats(channel: Float32Array, sampleRate: number, duration: number): { bpm: number; beatMarkers: BeatMarker[] } {
  // Build a short-time RMS envelope. Positive energy changes approximate percussive onsets.
  const frameSize = Math.max(256, Math.floor(sampleRate * 0.01)); // about 10 ms
  const envelope: number[] = [];
  for (let start = 0; start < channel.length; start += frameSize) {
    const end = Math.min(channel.length, start + frameSize);
    let sum = 0;
    for (let i = start; i < end; i++) sum += channel[i] * channel[i];
    envelope.push(Math.sqrt(sum / Math.max(1, end - start)));
  }
  const onset = envelope.map((value, i) => Math.max(0, value - (envelope[i - 1] || 0)));
  const mean = onset.reduce((a, b) => a + b, 0) / Math.max(1, onset.length);
  const centered = onset.map(v => v - mean);
  const fps = sampleRate / frameSize;

  // Autocorrelation over 60–180 BPM. Use half-beat and beat-scale periodicity.
  let bestBpm = 120;
  let bestScore = -Infinity;
  for (let bpm = 60; bpm <= 180; bpm++) {
    const lag = Math.round(fps * 60 / bpm);
    if (lag < 1 || lag >= centered.length) continue;
    let score = 0;
    let count = 0;
    for (let i = lag; i < centered.length; i++) {
      score += centered[i] * centered[i - lag];
      count++;
    }
    score /= Math.max(1, count);
    if (score > bestScore) { bestScore = score; bestBpm = bpm; }
  }

  // Find onset peaks, suppressing adjacent peaks within ~120 ms.
  const localPeaks: Array<{ timestamp: number; strength: number }> = [];
  const threshold = Math.max(mean * 1.8, ...onset) * 0.22;
  const minGap = Math.max(1, Math.round(fps * 0.12));
  let lastPeak = -minGap;
  for (let i = 1; i < onset.length - 1; i++) {
    if (onset[i] >= threshold && onset[i] >= onset[i - 1] && onset[i] > onset[i + 1] && i - lastPeak >= minGap) {
      localPeaks.push({ timestamp: Number((i / fps).toFixed(2)), strength: onset[i] });
      lastPeak = i;
    }
  }

  // If transient detection is weak, use the measured BPM grid rather than random/fake markers.
  const interval = 60 / bestBpm;
  const markers: BeatMarker[] = [];
  if (localPeaks.length >= 3) {
    const strongest = [...localPeaks].sort((a, b) => b.strength - a.strength);
    const chosen: typeof localPeaks = [];
    for (const peak of strongest) {
      if (chosen.every(p => Math.abs(p.timestamp - peak.timestamp) > 0.18)) chosen.push(peak);
    }
    chosen.sort((a, b) => a.timestamp - b.timestamp);
    chosen.forEach((peak, index) => {
      markers.push({
        id: 'audio-beat-' + index + '-' + peak.timestamp.toFixed(2),
        timestamp: peak.timestamp,
        intensity: Math.max(0.1, Math.min(1, peak.strength / Math.max(...chosen.map(p => p.strength), 0.001))),
        isBeatDrop: peak.strength >= Math.max(...chosen.map(p => p.strength), 0.001) * 0.78,
      });
    });
  } else {
    for (let t = 0, i = 0; t < duration; t += interval, i++) {
      markers.push({ id: 'audio-grid-' + i, timestamp: Number(t.toFixed(2)), intensity: 0.55, isBeatDrop: i % 4 === 0 });
    }
  }
  return { bpm: bestBpm, beatMarkers: markers };
}

export const SAMPLE_SONGS: SongTrack[] = [
  {
    id: 'song-1',
    title: 'Neon Horizon (demo)',
    artist: 'Demo Track',
    duration: 30,
    bpm: 124,
    waveformData: Array.from({ length: 50 }, (_, i) => Number((0.25 + 0.65 * Math.abs(Math.sin(i * 0.37))).toFixed(3))),
    beatMarkers: Array.from({ length: 16 }, (_, i) => ({
      id: 'demo-beat-' + i, timestamp: Number((i * 60 / 124).toFixed(2)),
      intensity: i % 4 === 0 ? 1 : 0.6, isBeatDrop: i % 4 === 0,
    })),
  },
  {
    id: 'song-2',
    title: 'Midnight Drive (demo)',
    artist: 'Demo Track',
    duration: 30,
    bpm: 118,
    waveformData: Array.from({ length: 50 }, (_, i) => Number((0.2 + 0.7 * Math.abs(Math.sin(i * 0.29 + 0.6))).toFixed(3))),
    beatMarkers: Array.from({ length: 16 }, (_, i) => ({
      id: 'demo-drive-beat-' + i, timestamp: Number((i * 60 / 118).toFixed(2)),
      intensity: i % 4 === 0 ? 1 : 0.6, isBeatDrop: i % 4 === 0,
    })),
  },
];

export async function analyzeAudioFile(file: File): Promise<SongTrack> {
  const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
  if (!AudioContextClass) throw new Error('This browser does not support audio analysis. Try a current Chrome browser.');
  const context: AudioContext = new AudioContextClass();
  try {
    const decoded = await context.decodeAudioData(await file.arrayBuffer());
    if (!decoded.numberOfChannels || decoded.length < 1) throw new Error('The selected audio file has no readable audio.');
    const channel = decoded.getChannelData(0);
    const duration = Number(decoded.duration.toFixed(2));
    const waveformData = buildWaveform(channel);
    const { bpm, beatMarkers } = analyzeBeats(channel, decoded.sampleRate, duration);
    return {
      id: 'song-' + Date.now(),
      title: file.name.replace(/\.[^/.]+$/, ''),
      artist: 'Imported audio',
      duration,
      bpm,
      audioUrl: URL.createObjectURL(file),
      waveformData,
      beatMarkers,
    };
  } catch (error) {
    throw new Error(error instanceof Error ? 'Audio analysis failed: ' + error.message : 'Audio analysis failed. Please try an MP3, WAV, or M4A file.');
  } finally {
    await context.close().catch(() => undefined);
  }
}
