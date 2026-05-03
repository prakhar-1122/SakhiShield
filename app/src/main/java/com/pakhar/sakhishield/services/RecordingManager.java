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
    public static final String PREF_AUTO_AUDIO = "autoAudioEnabled";
    public static final String PREF_AUTO_VIDEO = "autoVideoEnabled";

    public RecordingManager(Context context) {
        this.context = context;
    }

    public boolean isAutoAudioEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(
                "SakhiShieldPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_AUDIO, true);
    }
    public boolean isAutoVideoEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(
                "SakhiShieldPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_VIDEO, false);
    }
    public void startEmergencyRecording() {
        if (isAutoAudioEnabled()) {
            startAudio();
        }
        if (isAutoVideoEnabled()) {
            startVideo();
        }
    }
    public void stopAllRecordings() {
        stopAudio();
        stopVideo();
    }
    public boolean isRecording() {
        return isAudioRecording || isVideoRecording;
    }
    public void release() {
        stopAudio();
        stopVideo();
    }

    private void startAudio() {
        if (isAudioRecording) return;

        try {
            String fileName = "Emergency_Audio_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date()) + ".3gp";
            String filePath = getFilePath(fileName, false);
            audioRecorder = new MediaRecorder(context);
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // microphone input
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); // audio codec
            audioRecorder.setOutputFile(filePath);
            audioRecorder.prepare();
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
                audioRecorder.stop();
                audioRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                audioRecorder = null;
                isAudioRecording = false;
            }
        }
    }

    private void startVideo() {
        // Don't start if already recording
        if (isVideoRecording) return;
        try {
            // Generate timestamped filename
            String fileName = "Emergency_Video_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date()) + ".mp4";
            String filePath = getFilePath(fileName, true);
            videoRecorder = new MediaRecorder(context);
            videoRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // mic for audio track
            videoRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // .mp4 container
            videoRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
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
    private String getFilePath(String fileName, boolean isVideo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File dir = isVideo
                    ? context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    : context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (dir != null && !dir.exists()) dir.mkdirs();
            return new File(dir, fileName).getAbsolutePath();
        } else {
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