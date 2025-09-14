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
            // โหลดเสียง slash1.wav
            loadSound("slash", "assets/sounds/slash1.wav");
            
            // โหลดเสียง footsteps.wav
            loadSound("footsteps", "assets/sounds/footsteps.wav");
            
            // เก็บ footstep clip แยกเพื่อควบคุมการเล่น/หยุด
            footstepClip = soundClips.get("footsteps");
            if (footstepClip != null) {
                // ตั้งให้เสียง footsteps วนซ้ำ
                footstepClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            
        } catch (Exception e) {
            System.out.println("ไม่สามารถโหลดเสียงได้: " + e.getMessage());
        }
    }
    
    private void loadSound(String name, String path) {
        try {
            File soundFile = new File(path);
            System.out.println("กำลังโหลดเสียง " + name + " จาก: " + path);
            System.out.println("ไฟล์มีอยู่จริง: " + soundFile.exists());
            System.out.println("ขนาดไฟล์: " + soundFile.length() + " bytes");
            
            if (soundFile.exists()) {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                soundClips.put(name, clip);
                System.out.println("โหลดเสียง " + name + " สำเร็จ - Duration: " + clip.getMicrosecondLength() / 1000000.0 + " วินาที");
            } else {
                System.out.println("ไม่พบไฟล์เสียง: " + path);
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("เกิดข้อผิดพลาดในการโหลดเสียง " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void playSlashSound() {
        Clip clip = soundClips.get("slash");
        if (clip != null) {
            // รีเซ็ตตำแหน่งการเล่นกลับไปที่จุดเริ่มต้น
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void startFootstepSound() {
        if (footstepClip != null && !isFootstepPlaying) {
            footstepClip.setFramePosition(0);
            footstepClip.start();
            isFootstepPlaying = true;
            System.out.println("เสียง footsteps เริ่มเล่นแล้ว");
        } else {
            System.out.println("ไม่สามารถเล่นเสียง footsteps ได้ - Clip: " + (footstepClip != null) + " Playing: " + isFootstepPlaying);
        }
    }
    
    public void stopFootstepSound() {
        if (footstepClip != null && isFootstepPlaying) {
            footstepClip.stop();
            isFootstepPlaying = false;
            System.out.println("เสียง footsteps หยุดแล้ว");
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
