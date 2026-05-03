package com.pakhar.sakhishield.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.database.DatabaseHelper;

public class LoginActivity extends AppCompatActivity {
    EditText phoneInput, passwordInput;
    Button loginBtn;
    TextView registerLink;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("SakhiShieldPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        phoneInput   = findViewById(R.id.loginPhone);
        passwordInput = findViewById(R.id.loginPassword);
        loginBtn     = findViewById(R.id.loginBtn);
        registerLink = findViewById(R.id.registerLink);
        loginBtn.setOnClickListener(v -> {
            String phone    = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dbHelper.checkUser(phone, password)) {
                prefs.edit()
                        .putBoolean("isLoggedIn", true)
                        .putString("userPhone", phone)
                        .apply();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid phone or password", Toast.LENGTH_SHORT).show();
            }
        });
        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}