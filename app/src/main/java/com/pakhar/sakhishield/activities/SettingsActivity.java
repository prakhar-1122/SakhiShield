package com.pakhar.sakhishield.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.database.DatabaseHelper;
import com.pakhar.sakhishield.services.RecordingManager;

public class SettingsActivity extends AppCompatActivity {

    // Profile edit fields
    EditText editName, editEmail, editOldPassword,
            editNewPassword, editConfirmPassword;
    Button saveProfileBtn;
    TextView currentUserInfo;

    // Recording toggles
    SwitchCompat autoAudioSwitch, autoVideoSwitch;
    DatabaseHelper dbHelper;
    SharedPreferences prefs;
    String userPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);
        prefs    = getSharedPreferences("SakhiShieldPrefs", MODE_PRIVATE);
        userPhone = prefs.getString("userPhone", "");

        // Bind views
        currentUserInfo     = findViewById(R.id.currentUserInfo);
        editName            = findViewById(R.id.editName);
        editEmail           = findViewById(R.id.editEmail);
        editOldPassword     = findViewById(R.id.editOldPassword);
        editNewPassword     = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        saveProfileBtn      = findViewById(R.id.saveProfileBtn);
        autoAudioSwitch     = findViewById(R.id.autoAudioSwitch);
        autoVideoSwitch     = findViewById(R.id.autoVideoSwitch);

        loadCurrentProfile();
        loadSwitchStates();

        // Save profile button
        saveProfileBtn.setOnClickListener(v -> saveProfile());

        // Auto audio toggle
        autoAudioSwitch.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) -> {
                    prefs.edit().putBoolean(
                            RecordingManager.PREF_AUTO_AUDIO, isChecked).apply();
                    Toast.makeText(this,
                            "Auto audio " + (isChecked ? "enabled" : "disabled"),
                            Toast.LENGTH_SHORT).show();
                });

        // Auto video toggle
        autoVideoSwitch.setOnCheckedChangeListener(
                (CompoundButton buttonView, boolean isChecked) -> {
                    prefs.edit().putBoolean(
                            RecordingManager.PREF_AUTO_VIDEO, isChecked).apply();
                    Toast.makeText(this,
                            "Auto video " + (isChecked ? "enabled" : "disabled"),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ─── LOAD CURRENT DATA ────────────────────────────────────────

    private void loadCurrentProfile() {
        if (userPhone.isEmpty()) return;
        String[] details = dbHelper.getUserDetails(userPhone);
        String name  = details[0];
        String email = details[2];

        // Show current info
        currentUserInfo.setText("Current: " + name + " | " + userPhone +
                "\n" + (email.isEmpty() ? "No email" : email));

        // Pre-fill editable fields
        editName.setText(name);
        editEmail.setText(email);
    }

    private void loadSwitchStates() {
        // Load saved toggle states
        autoAudioSwitch.setChecked(
                prefs.getBoolean(RecordingManager.PREF_AUTO_AUDIO, true));
        autoVideoSwitch.setChecked(
                prefs.getBoolean(RecordingManager.PREF_AUTO_VIDEO, false));
    }

    // ─── SAVE PROFILE ─────────────────────────────────────────────

    private void saveProfile() {
        String newName    = editName.getText().toString().trim();
        String newEmail   = editEmail.getText().toString().trim();
        String oldPass    = editOldPassword.getText().toString().trim();
        String newPass    = editNewPassword.getText().toString().trim();
        String confirmPass = editConfirmPassword.getText().toString().trim();

        // Validate name
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email if provided
        if (!newEmail.isEmpty() &&
                !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Invalid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle password change — only if user filled old password
        if (!oldPass.isEmpty()) {
            if (!dbHelper.checkUser(userPhone, oldPass)) {
                Toast.makeText(this,
                        "Current password is incorrect.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(this,
                        "New password must be at least 6 characters.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this,
                        "New passwords do not match.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // Update with new password
            dbHelper.updateUser(userPhone, newName, newEmail, newPass);
        } else {
            // Update name and email only — keep existing password
            dbHelper.updateUser(userPhone, newName, newEmail, null);
        }

        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

        // Clear password fields
        editOldPassword.setText("");
        editNewPassword.setText("");
        editConfirmPassword.setText("");

        // Refresh displayed info
        loadCurrentProfile();
    }
}