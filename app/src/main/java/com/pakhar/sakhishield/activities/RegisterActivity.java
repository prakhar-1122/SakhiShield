package com.pakhar.sakhishield.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.database.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {
    EditText nameInput, phoneInput, emailInput, passwordInput, confirmPasswordInput;
    Button registerBtn;
    TextView loginLink;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        dbHelper = new DatabaseHelper(this);
        nameInput            = findViewById(R.id.registerName);
        phoneInput           = findViewById(R.id.registerPhone);
        emailInput           = findViewById(R.id.registerEmail);
        passwordInput        = findViewById(R.id.registerPassword);
        confirmPasswordInput = findViewById(R.id.registerConfirmPassword);
        registerBtn          = findViewById(R.id.registerBtn);
        loginLink            = findViewById(R.id.loginLink);
        registerBtn.setOnClickListener(v -> {
            String name     = nameInput.getText().toString().trim();
            String phone    = phoneInput.getText().toString().trim();
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirm  = confirmPasswordInput.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty() || email.isEmpty()
                    || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() < 10 || !phone.matches("[0-9]+")) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean registered = dbHelper.registerUser(name, phone, email, password);
            if (registered) {
                Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Phone number already registered", Toast.LENGTH_SHORT).show();
            }
        });
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}