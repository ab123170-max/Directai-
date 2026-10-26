import { MusicVideoShot, SongTrack, ShootingStyle } from '../types';

export function generateAIShotList(
  song: SongTrack,
  style: ShootingStyle,
  targetDuration: number
): MusicVideoShot[] {
  const beatTimes = song.beatMarkers.map(b => b.timestamp);
  
  const templates: Record<ShootingStyle, Array<{ name: string; angle: string; movement: any; action: string; framing: string; fps: number; transition: string }>> = {
    'Hero and attitude': [
      { name: 'Establishing Hero Walk', angle: 'Low-angle', movement: 'Move closer', action: 'Walking confidently toward camera', framing: 'Full body tracking shot', fps: 60, transition: 'Hard cut' },
      { name: 'Rhythmic Head Turn', angle: 'Eye-level', movement: 'Hold steady', action: 'Direct eye contact with beat drop', framing: 'Medium close-up portrait', fps: 24, transition: 'Zoom in' },
      { name: 'Dynamic Slow-Mo Turn', angle: 'Dutch tilt', movement: 'Move around subject', action: 'Sharp 360 degree pivot', framing: 'Dynamic waist-up shot', fps: 120, transition: 'Cross dissolve' },
      { name: 'Climax Power Pose', angle: 'Low-angle', movement: 'Tilt upward', action: 'Striking signature pose on heavy beat', framing: 'Dramatic hero angle', fps: 60, transition: 'Fade to black' },
    ],
    'Dance': [
      { name: 'Groove Establishing', angle: 'Eye-level', movement: 'Pan left', action: 'Full choreography opening step', framing: 'Wide cinematic dance frame', fps: 60, transition: 'Whip pan' },
      { name: 'Footwork Macro', angle: 'Low-angle', movement: 'Hold steady', action: 'Intricate footwork synchronized with BPM', framing: 'Extreme close-up on shoes', fps: 120, transition: 'Hard cut' },
      { name: 'Orbiting Spin', angle: 'Eye-level', movement: 'Move around subject', action: 'Dynamic spinning movement', framing: 'Medium full shot', fps: 60, transition: 'Cross dissolve' },
      { name: 'Final Freeze Frame', angle: 'Eye-level', movement: 'Hold steady', action: 'Final dramatic freeze on beat drop', framing: 'Center-framed portrait', fps: 30, transition: 'Fade out' },
    ],
    'Luxury cinematic': [
      { name: 'Atmospheric Detail', angle: 'Macro', movement: 'Pan right', action: 'Slow pan across luxury texture', framing: 'Macro detail shot', fps: 24, transition: 'Cross dissolve' },
      { name: 'Graceful Approach', angle: 'Eye-level', movement: 'Move closer', action: 'Calm, effortless approach', framing: 'Medium cinematic shot', fps: 24, transition: 'Cross dissolve' },
      { name: 'Reflective Profile', angle: 'Profile', movement: 'Hold steady', action: 'Subtle contemplative expression', framing: 'Shallow depth of field close-up', fps: 24, transition: 'Fade to black' },
    ],
    'Couple': [
      { name: 'Romantic Introduction', angle: 'Eye-level', movement: 'Pan left', action: 'Shared smile and gentle movement', framing: 'Two-shot medium frame', fps: 30, transition: 'Cross dissolve' },
      { name: 'Close Connection', angle: 'Close-up', movement: 'Hold steady', action: 'Forehead touch on beat accent', framing: 'Tight couple portrait', fps: 24, transition: 'Cross dissolve' },
      { name: 'Playful Spin', angle: 'Low-angle', movement: 'Move around subject', action: 'Laughter and spinning embrace', framing: 'Wide dynamic shot', fps: 60, transition: 'Hard cut' },
    ],
    'Street cinematic': [
      { name: 'Urban Atmosphere', angle: 'Wide angle', movement: 'Move backward', action: 'Walking through neon city lights', framing: 'Wide urban landscape', fps: 60, transition: 'Whip pan' },
      { name: 'Confident Stride', angle: 'Low-angle', movement: 'Move closer', action: 'Fast-paced street walk', framing: 'Low-angle tracking shot', fps: 60, transition: 'Hard cut' },
      { name: 'Shadow & Light', angle: 'Side profile', movement: 'Hold steady', action: 'Pausing under streetlamp', framing: 'High-contrast cinematic close-up', fps: 24, transition: 'Fade out' },
    ],
    'Nature and travel': [
      { name: 'Scenic Vistas', angle: 'Wide angle', movement: 'Pan right', action: 'Sweeping landscape panorama', framing: 'Ultra-wide establishing shot', fps: 30, transition: 'Cross dissolve' },
      { name: 'In-Motion Journey', angle: 'Eye-level', movement: 'Move forward', action: 'Advancing along trail or road', framing: 'POV tracking shot', fps: 60, transition: 'Hard cut' },
      { name: 'Golden Hour Reflection', angle: 'Low-angle', movement: 'Hold steady', action: 'Serene pause in nature', framing: 'Symmetric framing shot', fps: 24, transition: 'Fade to black' },
    ],
    'Action-inspired': [
      { name: 'Adrenaline Rush Start', angle: 'Dutch tilt', movement: 'Move closer', action: 'Rapid motion toward camera', framing: 'Dynamic action shot', fps: 120, transition: 'Whip pan' },
      { name: 'High-Energy Burst', angle: 'Low-angle', movement: 'Tilt upward', action: 'Explosive movement on beat drop', framing: 'Wide power framing', fps: 60, transition: 'Hard cut' },
      { name: 'Intense Focus', angle: 'Extreme close-up', movement: 'Hold steady', action: 'Piercing eye contact', framing: 'Tight macro portrait', fps: 60, transition: 'Fade out' },
    ],
    'Food and lifestyle': [
      { name: 'Appetizing Establish', angle: 'Overhead', movement: 'Pan left', action: 'Preparation and ingredient arrangement', framing: 'Top-down flat lay view', fps: 30, transition: 'Cross dissolve' },
      { name: 'Sizzling Action', angle: 'Close-up', movement: 'Move closer', action: 'Pouring or stirring with steam', framing: 'Macro detail shot', fps: 60, transition: 'Hard cut' },
      { name: 'Hero Plated Reveal', angle: '45-degree angle', movement: 'Hold steady', action: 'Final perfect presentation', framing: 'Cinematic tabletop hero shot', fps: 30, transition: 'Fade out' },
    ],
  };

  const selectedTemplate = templates[style] || templates['Hero and attitude'];
  const shotDuration = targetDuration / selectedTemplate.length;

  return selectedTemplate.map((item, index) => {
    const startTime = Number((index * shotDuration).toFixed(2));
    const endTime = Number(((index + 1) * shotDuration).toFixed(2));
    const beat = beatTimes[index % beatTimes.length] || startTime;

    return {
      id: `shot-${Date.now()}-${index}`,
      shotNumber: index + 1,
      name: item.name,
      startTime,
      endTime,
      duration: Number(shotDuration.toFixed(1)),
      cameraAngle: item.angle,
      cameraMovement: item.movement,
      subjectAction: item.action,
      framingInstructions: item.framing,
      suggestedFps: item.fps,
      transitionNext: item.transition,
      beatMarkerId: `beat-${beat}`,
      status: index === 0 ? 'READY_TO_RECORD' : 'WAITING',
      takes: [],
    };
  });
}


// Calls the server-side Gemini endpoint. The API key must never be placed in browser code.
export async function generateAIShotListLive(
  song: SongTrack,
  style: ShootingStyle,
  targetDuration: number
): Promise<MusicVideoShot[]> {
  const response = await fetch('/api/ai/generate-shotlist', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title: song.title + ' — ' + style,
      category: style,
      description: 'Create a music-video shot sequence for ' + targetDuration +
        ' seconds. Song BPM: ' + song.bpm +
        '. Beat timestamps in seconds: ' + song.beatMarkers.map(b => b.timestamp.toFixed(2)).join(', ') +
        '. Match cuts and movement accents to beats. Return a practical sequence for a solo phone creator.',
    }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.success !== true || !Array.isArray(payload.shots) || payload.shots.length === 0) {
    throw new Error(payload.error || 'Gemini could not generate the shot plan. Check API configuration and try again.');
  }

  const total = Math.max(1, targetDuration);
  const raw = payload.shots as Array<{shotNumber?:number; name?:string; type?:string; purpose?:string; angle?:string; movement?:string; duration?:number; subject?:string}>;
  const weights = raw.map(s => Math.max(1, Number(s.duration) || 4));
  const weightTotal = weights.reduce((sum, value) => sum + value, 0);
  let cursor = 0;
  return raw.map((item, index) => {
    const startTime = Number(cursor.toFixed(2));
    const duration = index === raw.length - 1
      ? Number((total - cursor).toFixed(2))
      : Number((total * weights[index] / weightTotal).toFixed(2));
    cursor += duration;
    const movementMap: Record<string, any> = {
      'static': 'Hold steady', 'pan-left': 'Pan left', 'pan-right': 'Pan right',
      'tilt-up': 'Tilt upward', 'tilt-down': 'Tilt downward', 'push-in': 'Move closer',
      'pull-back': 'Move backward', 'orbit': 'Move around subject', 'track': 'Move closer',
      'reveal': 'Pan right',
    };
    const normalizedMovement = (item.movement || '').toLowerCase().replace(/_/g, '-');
    const beat = song.beatMarkers.reduce((best, marker) =>
      Math.abs(marker.timestamp - startTime) < Math.abs(best.timestamp - startTime) ? marker : best,
      song.beatMarkers[0] || { id: '', timestamp: startTime, intensity: 0, isBeatDrop: false });
    return {
      id: 'gemini-shot-' + Date.now() + '-' + index,
      shotNumber: index + 1,
      name: item.name || ('Cinematic Shot ' + (index + 1)),
      startTime,
      endTime: Number((startTime + duration).toFixed(2)),
      duration,
      cameraAngle: item.angle || 'Eye-level',
      cameraMovement: movementMap[normalizedMovement] || 'Hold steady',
      subjectAction: item.subject || item.purpose || 'Follow the shot direction and perform a natural action.',
      framingInstructions: item.purpose || ('Frame the subject clearly for a ' + (item.type || 'cinematic') + ' shot.'),
      suggestedFps: /slow|detail|close/i.test((item.type || '') + ' ' + (item.name || '')) ? 60 : 30,
      transitionNext: index === raw.length - 1 ? 'Fade out' : (beat.isBeatDrop ? 'Hard cut' : 'Cut on beat'),
      beatMarkerId: beat.id,
      status: index === 0 ? 'READY_TO_RECORD' : 'WAITING',
      takes: [],
    } as MusicVideoShot;
  });
}
