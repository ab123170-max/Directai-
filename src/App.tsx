/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { AppScreen, MusicVideoProject } from './types';
import { SAMPLE_SONGS } from './services/audioService';
import { generateAIShotList } from './services/aiDirectorService';

import { AndroidSplashScreen } from './components/AndroidSplashScreen';
import { AndroidHomeDashboard } from './components/AndroidHomeDashboard';
import { SongStudioScreen } from './components/SongStudioScreen';
import { ShotDirectorScreen } from './components/ShotDirectorScreen';
import { NativeCameraScreen } from './components/NativeCameraScreen';
import { VideoEditingStudioScreen } from './components/VideoEditingStudioScreen';
import { SettingsScreen } from './components/SettingsScreen';

const STORAGE_KEY = 'cameraman_ai_native_projects_v1';

const INITIAL_PROJECT: MusicVideoProject = {
  id: 'proj-1',
  title: 'Neon Horizon Music Video',
  song: SAMPLE_SONGS[0],
  shootingStyle: 'Hero and attitude',
  targetDuration: 30,
  location: 'Downtown Studio',
  crewSize: 'Solo Creator',
  lightingCondition: 'Studio Neon',
  skillLevel: 'Intermediate',
  shots: generateAIShotList(SAMPLE_SONGS[0], 'Hero and attitude', 30),
  createdAt: Date.now(),
  updatedAt: Date.now(),
};

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<AppScreen>('splash');
  const [projects, setProjects] = useState<MusicVideoProject[]>([INITIAL_PROJECT]);
  const [activeProject, setActiveProject] = useState<MusicVideoProject>(INITIAL_PROJECT);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setProjects(parsed);
          setActiveProject(parsed[0]);
        }
      }
    } catch (e) {}
  }, []);

  const handleUpdateProject = (updated: MusicVideoProject) => {
    setActiveProject(updated);
    const updatedList = projects.map(p => p.id === updated.id ? updated : p);
    setProjects(updatedList);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updatedList));
    } catch (e) {}
  };

  const handleCreateNew = () => {
    const newProj: MusicVideoProject = {
      id: `proj-${Date.now()}`,
      title: 'New Music Video',
      song: SAMPLE_SONGS[0],
      shootingStyle: 'Dance',
      targetDuration: 30,
      location: 'Urban Studio',
      crewSize: 'Solo',
      lightingCondition: 'Natural',
      skillLevel: 'Pro',
      shots: generateAIShotList(SAMPLE_SONGS[0], 'Dance', 30),
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    setProjects([newProj, ...projects]);
    setActiveProject(newProj);
    setCurrentScreen('songStudio');
  };

  return (
    <div className="min-h-screen bg-[#121212] text-zinc-100 font-['Plus_Jakarta_Sans',sans-serif] selection:bg-blue-600 selection:text-white">
      {currentScreen === 'splash' && (
        <AndroidSplashScreen onNavigate={setCurrentScreen} />
      )}

      {currentScreen === 'home' && (
        <AndroidHomeDashboard
          projects={projects}
          activeProject={activeProject}
          onSelectProject={(proj) => {
            setActiveProject(proj);
            setCurrentScreen('shotDirector');
          }}
          onCreateNew={handleCreateNew}
          onNavigate={setCurrentScreen}
        />
      )}

      {currentScreen === 'songStudio' && (
        <SongStudioScreen
          activeProject={activeProject}
          onUpdateProject={handleUpdateProject}
          onNavigate={setCurrentScreen}
        />
      )}

      {currentScreen === 'shotDirector' && (
        <ShotDirectorScreen
          activeProject={activeProject}
          onUpdateProject={handleUpdateProject}
          onNavigate={setCurrentScreen}
        />
      )}

      {currentScreen === 'camera' && (
        <NativeCameraScreen
          activeProject={activeProject}
          onUpdateProject={handleUpdateProject}
          onNavigate={setCurrentScreen}
        />
      )}

      {currentScreen === 'editing' && (
        <VideoEditingStudioScreen
          activeProject={activeProject}
          onUpdateProject={handleUpdateProject}
          onNavigate={setCurrentScreen}
        />
      )}

      {currentScreen === 'settings' && (
        <SettingsScreen onNavigate={setCurrentScreen} />
      )}
    </div>
  );
}
