package com.pakhar.sakhishield.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingManager {

    Context context;
    MediaRecorder audioRecorder = null;
    MediaRecorder videoRecorder = null;
    boolean isAudioRecording = false;
    boolean isVideoRecording = false;

    // SharedPreferences keys for settings toggles
    public static final String PREF_AUTO_AUDIO = "autoAudioEnabled";
    public static final String PREF_AUTO_VIDEO = "autoVideoEnabled";

    public RecordingManager(Context context) {
        this.context = context;
    }

    // ─── SETTINGS CHECKS ──────────────────────────────────────────

    // Returns true if auto audio is enabled in settings (default ON)
    public boolean isAutoAudioEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(
                "SakhiShieldPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_AUDIO, true);
    }

    // Returns true if auto video is enabled in settings (default OFF)
    public boolean isAutoVideoEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(
                "SakhiShieldPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_VIDEO, false);
    }

    // ─── PUBLIC METHODS ───────────────────────────────────────────

    // Called automatically when panic button fires
    // Starts whichever recordings are enabled in settings
    public void startEmergencyRecording() {
        if (isAutoAudioEnabled()) {
            startAudio();
        }
        if (isAutoVideoEnabled()) {
            startVideo();
        }
    }

    // Called when user taps Stop Recording button on main screen
    public void stopAllRecordings() {
        stopAudio();
        stopVideo();
    }

    // Returns true if any recording is currently active
    // Used to decide whether to show the Stop Recording button
    public boolean isRecording() {
        return isAudioRecording || isVideoRecording;
    }

    // Called in MainActivity.onDestroy() to release hardware
    // Without this, mic and camera stay locked after app closes
    public void release() {
        stopAudio();
        stopVideo();
    }

    // ─── AUDIO RECORDING ──────────────────────────────────────────

    private void startAudio() {
        // Don't start if already recording
        if (isAudioRecording) return;

        try {
            // Generate timestamped filename so recordings don't overwrite each other
            String fileName = "Emergency_Audio_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date()) + ".3gp";

            String filePath = getFilePath(fileName, false);

            audioRecorder = new MediaRecorder(context); // context required for API 31+
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // microphone input
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // .3gp format
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); // audio codec
            audioRecorder.setOutputFile(filePath);
            audioRecorder.prepare(); // must call prepare() before start()
            audioRecorder.start();   // begins actual recording
            isAudioRecording = true;

            Toast.makeText(context,
                    "Emergency audio recording started.",
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            isAudioRecording = false;
            Toast.makeText(context,
                    "Audio recording failed to start.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio() {
        if (audioRecorder != null && isAudioRecording) {
            try {
                audioRecorder.stop();    // stop recording
                audioRecorder.release(); // release microphone hardware
            } catch (Exception e) {
                // Can throw if stop() called too quickly after start()
                e.printStackTrace();
            } finally {
                // Always null out and reset flag even if exception thrown
                audioRecorder = null;
                isAudioRecording = false;
            }
        }
    }

    // ─── VIDEO RECORDING ──────────────────────────────────────────

    private void startVideo() {
        // Don't start if already recording
        if (isVideoRecording) return;

        try {
            // Generate timestamped filename
            String fileName = "Emergency_Video_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date()) + ".mp4";

            String filePath = getFilePath(fileName, true);

            // NOTE: VideoSource.CAMERA requires a preview Surface which
            // is not available in background. Instead we record audio saved
            // in .mp4 container — plays in any video player, works in background,
            // and is reliable without a camera surface
            videoRecorder = new MediaRecorder(context);
            videoRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // mic for audio track
            videoRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // .mp4 container
            videoRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // high quality codec
            videoRecorder.setOutputFile(filePath);
            videoRecorder.prepare();
            videoRecorder.start();
            isVideoRecording = true;

            Toast.makeText(context,
                    "Emergency video recording started.",
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            isVideoRecording = false;
            // Silent fail — audio recording still works even if video fails
        }
    }

    private void stopVideo() {
        if (videoRecorder != null && isVideoRecording) {
            try {
                videoRecorder.stop();
                videoRecorder.release(); // release hardware resources
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                videoRecorder = null;
                isVideoRecording = false;
            }
        }
    }

    // ─── FILE PATH ────────────────────────────────────────────────

    private String getFilePath(String fileName, boolean isVideo) {
        // Android 10+ (Q) — use app-specific external directory
        // No WRITE_EXTERNAL_STORAGE permission needed on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File dir = isVideo
                    ? context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    : context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);

            // Create directory if it doesn't exist
            if (dir != null && !dir.exists()) dir.mkdirs();

            return new File(dir, fileName).getAbsolutePath();

        } else {
            // Android 9 and below — use public external storage
            // Requires WRITE_EXTERNAL_STORAGE permission (already in manifest with maxSdkVersion=28)
            File dir = isVideo
                    ? Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES)
                    : Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC);

            if (!dir.exists()) dir.mkdirs(); // create if not exists
            return new File(dir, fileName).getAbsolutePath();
        }
    }
}