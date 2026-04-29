package com.pakhar.sakhishield.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.database.DatabaseHelper;
import com.pakhar.sakhishield.services.EmergencyManager;
import com.pakhar.sakhishield.services.EmergencyService;
import com.pakhar.sakhishield.services.RecordingManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CORE_PERMISSIONS = 1;
    private static final int REQUEST_NOTIFICATION = 2;
    private static final int REQUEST_BACKGROUND_LOCATION = 3;
    private static final int REQUEST_RECORDING_PERMISSIONS = 4;

    FusedLocationProviderClient fusedLocationClient;
    EmergencyManager emergencyManager;
    DatabaseHelper dbHelper;
    Button panicButton;
    TextView statusText;
    CountDownTimer countDownTimer;
    RecordingManager recordingManager;
    Button stopRecordingBtn;
    boolean isCountingDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        emergencyManager = new EmergencyManager(dbHelper, this);
        recordingManager = new RecordingManager(this);

        panicButton = findViewById(R.id.panicButton);
        statusText = findViewById(R.id.statusText);
        stopRecordingBtn = findViewById(R.id.stopRecordingBtn);

        Button settingsBtn = findViewById(R.id.settingsBtn);
        Button logoutBtn = findViewById(R.id.logoutBtn);

        LinearLayout contactRow = findViewById(R.id.contactRow);
        LinearLayout logRow = findViewById(R.id.logRow);
        LinearLayout homeRow = findViewById(R.id.homeRow);
        LinearLayout savedRecordingsRow = findViewById(R.id.savedRecordingsRow);

        TextView profileName = findViewById(R.id.profileName);
        TextView profileEmail = findViewById(R.id.profileEmail);
        TextView profilePhone = findViewById(R.id.profilePhone);
        TextView profileInitial = findViewById(R.id.profileInitial);

        SharedPreferences prefs = getSharedPreferences("SakhiShieldPrefs", MODE_PRIVATE);
        String userPhone = prefs.getString("userPhone", "");
        if (!userPhone.isEmpty()) {
            String[] details = dbHelper.getUserDetails(userPhone);
            String name = details[0];
            String phone = details[1];
            String email = details[2];
            profileName.setText(name);
            profileEmail.setText(email.isEmpty() ? "No email set" : email);
            profilePhone.setText(phone);
            if (!name.isEmpty()) {
                profileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            }
        }

        stopRecordingBtn.setVisibility(android.view.View.GONE);
        stopRecordingBtn.setOnClickListener(v -> {
            recordingManager.stopAllRecordings();
            stopRecordingBtn.setVisibility(android.view.View.GONE);
            statusText.setText("Recording stopped.");
            Toast.makeText(this, "Recording stopped.", Toast.LENGTH_SHORT).show();
        });

        panicButton.setOnClickListener(v -> {
            if (isCountingDown) cancelCountdown();
            else startCountdown();
        });

        settingsBtn.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        logoutBtn.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        contactRow.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));

        logRow.setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyLogActivity.class)));

        homeRow.setOnClickListener(v -> saveHomeLocation());

        // FIX #1: Was launching non-existent RecordingActivity — corrected to SavedRecordingsActivity
        savedRecordingsRow.setOnClickListener(v ->
                startActivity(new Intent(this, SavedRecordingsActivity.class)));

        requestCorePermissions();
    }

    // ─── PERMISSION FLOW ──────────────────────────────────────────

    private void requestCorePermissions() {
        boolean locationGranted = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean smsGranted = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean callGranted = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED;

        if (!locationGranted || !smsGranted || !callGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.SEND_SMS,
                            android.Manifest.permission.CALL_PHONE
                    }, REQUEST_CORE_PERMISSIONS);
        } else {
            requestRecordingPermissions();
        }
    }

    // FIX #2: Request RECORD_AUDIO and CAMERA at runtime
    private void requestRecordingPermissions() {
        boolean audioGranted = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean cameraGranted = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        if (!audioGranted || !cameraGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.CAMERA
                    }, REQUEST_RECORDING_PERMISSIONS);
        } else {
            requestBackgroundLocation();
        }
    }

    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        }, REQUEST_BACKGROUND_LOCATION);
            } else {
                requestNotificationAndStartService();
            }
        } else {
            requestNotificationAndStartService();
        }
    }

    private void requestNotificationAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION);
            } else {
                startEmergencyService();
            }
        } else {
            startEmergencyService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CORE_PERMISSIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Permissions granted. App is ready.");
            } else {
                statusText.setText("Some permissions denied. Emergency features limited.");
                Toast.makeText(this,
                        "Please grant all permissions for full functionality.",
                        Toast.LENGTH_LONG).show();
            }
            requestRecordingPermissions();

        } else if (requestCode == REQUEST_RECORDING_PERMISSIONS) {
            requestBackgroundLocation();

        } else if (requestCode == REQUEST_BACKGROUND_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Background location enabled.");
            } else {
                Toast.makeText(this,
                        "Background location denied. Continuous tracking disabled.",
                        Toast.LENGTH_SHORT).show();
            }
            requestNotificationAndStartService();

        } else if (requestCode == REQUEST_NOTIFICATION) {
            startEmergencyService();
        }
    }

    // ─── BACKGROUND SERVICE ───────────────────────────────────────

    private void startEmergencyService() {
        Intent serviceIntent = new Intent(this, EmergencyService.class);
        startForegroundService(serviceIntent);
    }

    // ─── COUNTDOWN ────────────────────────────────────────────────

    private void startCountdown() {
        isCountingDown = true;
        vibratePhone(500);
        panicButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FF5722")));

        countDownTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000) + 1;
                panicButton.setText("SENDING\n" + secondsLeft);
                statusText.setText("Tap again to cancel...");
                vibratePhone(200);
            }

            @Override
            public void onFinish() {
                isCountingDown = false;
                panicButton.setText("PANIC");
                panicButton.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#E91E63")));
                statusText.setText("Sending emergency alert...");
                vibratePhone(1000);

                // FIX #8: Actually start recording when panic fires
                recordingManager.startEmergencyRecording();
                if (recordingManager.isRecording()) {
                    stopRecordingBtn.setVisibility(android.view.View.VISIBLE);
                }

                getLocation();
            }
        }.start();
    }

    private void cancelCountdown() {
        if (countDownTimer != null) countDownTimer.cancel();
        isCountingDown = false;
        panicButton.setText("PANIC");
        panicButton.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#E91E63")));
        statusText.setText("Alert cancelled.");
        vibratePhone(100);
    }

    // ─── VIBRATION ────────────────────────────────────────────────

    private void vibratePhone(int milliseconds) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(
                    milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    // ─── LOCATION ─────────────────────────────────────────────────

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestCorePermissions();
            statusText.setText("Please grant permissions first.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    double lat, lon;
                    if (location != null) {
                        lat = location.getLatitude();
                        lon = location.getLongitude();
                    } else {
                        lat = 30.2675;
                        lon = 77.9959;
                        Toast.makeText(this,
                                "Emulator: Using mock location",
                                Toast.LENGTH_SHORT).show();
                    }
                    emergencyManager.sendEmergency(lat, lon);
                    emergencyManager.makeCall();

                    String time = new java.text.SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            java.util.Locale.getDefault())
                            .format(new java.util.Date());
                    statusText.setText("Last alert sent: " + time);
                });
    }

    // ─── SAVE HOME LOCATION ───────────────────────────────────────

    private void saveHomeLocation() {
        SharedPreferences prefs = getSharedPreferences("SakhiShieldPrefs", MODE_PRIVATE);
        String userPhone = prefs.getString("userPhone", "");

        if (userPhone.isEmpty()) return;

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,
                    "Location permission needed to save home location.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat, lon;
            if (location != null) {
                lat = location.getLatitude();
                lon = location.getLongitude();
            } else {
                lat = 30.2675;
                lon = 77.9959;
            }

            // FIX #6: Geocoder must run on a background thread
            final double finalLat = lat;
            final double finalLon = lon;
            new Thread(() -> {
                try {
                    android.location.Geocoder geocoder =
                            new android.location.Geocoder(this, java.util.Locale.getDefault());
                    java.util.List<android.location.Address> addresses =
                            geocoder.getFromLocation(finalLat, finalLon, 1);
                    String address = "";
                    if (addresses != null && !addresses.isEmpty()) {
                        address = addresses.get(0).getAddressLine(0);
                    }
                    final String finalAddress = address;
                    dbHelper.saveHomeLocation(userPhone, finalLat, finalLon, finalAddress);
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Home location saved!\n" + finalAddress,
                                Toast.LENGTH_LONG).show();
                        statusText.setText("Home saved: " + finalAddress);
                    });
                } catch (Exception e) {
                    dbHelper.saveHomeLocation(userPhone, finalLat, finalLon, "");
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Home location saved!", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }

    // ─── LIFECYCLE ────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        recordingManager.release();
    }
}
