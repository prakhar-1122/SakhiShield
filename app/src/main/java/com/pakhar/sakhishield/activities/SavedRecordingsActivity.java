package com.pakhar.sakhishield.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.pakhar.sakhishield.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class SavedRecordingsActivity extends AppCompatActivity {

    ListView recordingsList;
    TextView recordingsCount;
    Button clearAllBtn;

    ArrayList<File> recordingFiles = new ArrayList<>();
    ArrayList<String> recordingNames = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_recordings);

        recordingsList  = findViewById(R.id.recordingsList);
        recordingsCount = findViewById(R.id.recordingsCount);
        clearAllBtn     = findViewById(R.id.clearAllBtn);

        loadRecordings();

        // ─── TAP — PLAY/VIEW RECORDING ────────────────────────────
        recordingsList.setOnItemClickListener((parent, view, position, id) -> {
            File file = recordingFiles.get(position);
            openRecording(file);
        });

        // ─── LONG PRESS — DELETE ──────────────────────────────────
        recordingsList.setOnItemLongClickListener((parent, view, position, id) -> {
            File file = recordingFiles.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Recording")
                    .setMessage("Delete " + file.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (file.delete()) {
                            Toast.makeText(this,
                                    "Recording deleted.",
                                    Toast.LENGTH_SHORT).show();
                            loadRecordings();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        // ─── CLEAR ALL ────────────────────────────────────────────
        clearAllBtn.setOnClickListener(v -> {
            if (recordingFiles.isEmpty()) {
                Toast.makeText(this,
                        "No recordings to clear.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Clear All")
                    .setMessage("Delete all " + recordingFiles.size() + " recordings?")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        int deleted = 0;
                        for (File f : recordingFiles) {
                            if (f.delete()) deleted++;
                        }
                        Toast.makeText(this,
                                deleted + " recordings deleted.",
                                Toast.LENGTH_SHORT).show();
                        loadRecordings();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ─── OPEN RECORDING ───────────────────────────────────────────

    private void openRecording(File file) {
        try {
            // FileProvider — required on Android 7+ to share files securely
            // Direct file:// URIs are blocked by Android since API 24
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider", // matches authority in manifest
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);

            // Set correct MIME type based on file extension
            if (file.getName().endsWith(".mp4") || file.getName().endsWith(".avi")) {
                intent.setDataAndType(fileUri, "video/*");
            } else {
                // .3gp, .mp3, .aac etc
                intent.setDataAndType(fileUri, "audio/*");
            }

            // Grant temporary read permission to the media player app
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Open with any compatible media player on device
            startActivity(Intent.createChooser(intent, "Open with..."));

        } catch (Exception e) {
            Toast.makeText(this,
                    "No media player found to open this file.",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ─── LOAD RECORDINGS ──────────────────────────────────────────

    private void loadRecordings() {
        recordingFiles.clear();
        recordingNames.clear();

        // Scan both audio and video directories
        scanFolder(getExternalFilesDir(Environment.DIRECTORY_MUSIC));
        scanFolder(getExternalFilesDir(Environment.DIRECTORY_MOVIES));

        // Sort by newest first
        Collections.sort(recordingFiles,
                (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        // Build display names
        for (File f : recordingFiles) {
            boolean isVideo = f.getName().endsWith(".mp4") || f.getName().endsWith(".avi");
            String type = isVideo ? "🎥 Video" : "🎵 Audio";
            long sizeKb = f.length() / 1024;
            String date = new java.text.SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    java.util.Locale.getDefault())
                    .format(new java.util.Date(f.lastModified()));
            recordingNames.add(type + " — " + date +
                    "\n" + sizeKb + " KB  •  Tap to play");
        }

        // Update count text
        int count = recordingFiles.size();
        if (count == 0) {
            recordingsCount.setText("No recordings found.");
        } else {
            recordingsCount.setText(count + " recording" +
                    (count > 1 ? "s" : "") +
                    " saved. Tap to play, long press to delete.");
        }

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, recordingNames);
        recordingsList.setAdapter(adapter);
    }

    private void scanFolder(File folder) {
        if (folder == null || !folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            // Only include SakhiShield recordings — bracket fixes operator precedence
            if (f.isFile() && (f.getName().startsWith("SakhiShield") ||
                    f.getName().startsWith("Emergency"))) {
                recordingFiles.add(f);
            }
        }
    }
}