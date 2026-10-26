export type AppScreen = 
  | 'splash'
  | 'feed'
  | 'home'
  | 'songStudio'
  | 'shotDirector'
  | 'camera'
  | 'pipeline'
  | 'editing'
  | 'settings';

export type ShootingStyle = 
  | 'Hero and attitude'
  | 'Dance'
  | 'Luxury cinematic'
  | 'Couple'
  | 'Street cinematic'
  | 'Nature and travel'
  | 'Action-inspired'
  | 'Food and lifestyle';

export type ShotPipelineStatus = 
  | 'WAITING'
  | 'PREPARING'
  | 'READY_TO_RECORD'
  | 'RECORDING'
  | 'PROCESSING'
  | 'VERIFYING'
  | 'COMPLETED'
  | 'FAILED'
  | 'RETRY_REQUIRED'
  | 'SKIPPED';

export type CameraMovement = 
  | 'Move closer'
  | 'Move backward'
  | 'Pan left'
  | 'Pan right'
  | 'Tilt upward'
  | 'Tilt downward'
  | 'Move around subject'
  | 'Hold steady'
  | 'Start recording'
  | 'Finish shot';

export interface BeatMarker {
  id: string;
  timestamp: number;
  intensity: number;
  isBeatDrop: boolean;
}

export interface SongTrack {
  id: string;
  title: string;
  artist: string;
  duration: number;
  bpm: number;
  audioUrl?: string;
  waveformData: number[];
  beatMarkers: BeatMarker[];
}

export interface MusicVideoShot {
  id: string;
  shotNumber: number;
  name: string;
  startTime: number;
  endTime: number;
  duration: number;
  cameraAngle: string;
  cameraMovement: CameraMovement;
  subjectAction: string;
  framingInstructions: string;
  suggestedFps: number;
  transitionNext: string;
  beatMarkerId?: string;
  status: ShotPipelineStatus;
  recordedClipUrl?: string;
  takes: string[];
}

export interface MusicVideoProject {
  id: string;
  title: string;
  song: SongTrack;
  shootingStyle: ShootingStyle;
  targetDuration: number;
  location: string;
  crewSize: string;
  lightingCondition: string;
  skillLevel: string;
  shots: MusicVideoShot[];
  createdAt: number;
  updatedAt: number;
}
