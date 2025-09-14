package client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;

public class SoundManager {
    private final Map<String, Clip> soundClips;
    private Clip footstepClip;
    private boolean isFootstepPlaying;
    
    public SoundManager() {
        soundClips = new HashMap<>();
        isFootstepPlaying = false;
        loadSounds();
    }
    
    private void loadSounds() {
        try {
            loadSound("slash", "assets/sounds/slash1.wav");
            loadSound("footsteps", "assets/sounds/footsteps.wav");
            
            footstepClip = soundClips.get("footsteps");
            
        } catch (Exception e) {
            System.out.println("Failed to load sounds: " + e.getMessage());
        }
    }
    
    private void loadSound(String name, String path) {
        try {
            File soundFile = new File(path);
            if (soundFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                soundClips.put(name, clip);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Failed to load sound " + name + ": " + e.getMessage());
        }
    }
    
    public void playSlashSound() {
        Clip clip = soundClips.get("slash");
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void startFootstepSound() {
        if (footstepClip != null && !isFootstepPlaying) {
            footstepClip.setFramePosition(0);
            footstepClip.loop(Clip.LOOP_CONTINUOUSLY);
            isFootstepPlaying = true;
        }
    }
    
    public void stopFootstepSound() {
        if (footstepClip != null && isFootstepPlaying) {
            footstepClip.stop();
            isFootstepPlaying = false;
        }
    }
    
    public boolean isFootstepPlaying() {
        return isFootstepPlaying;
    }
    
    public void cleanup() {
        for (Clip clip : soundClips.values()) {
            if (clip != null && clip.isOpen()) {
                clip.close();
            }
        }
        soundClips.clear();
    }
}
