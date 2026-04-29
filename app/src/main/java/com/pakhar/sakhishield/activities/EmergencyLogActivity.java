package com.pakhar.sakhishield.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.database.DatabaseHelper;

import java.util.ArrayList;

public class EmergencyLogActivity extends AppCompatActivity {

    ListView logListView;
    TextView logCount;
    Button clearBtn;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_log);

        dbHelper    = new DatabaseHelper(this);
        logListView = findViewById(R.id.logListView);
        logCount    = findViewById(R.id.logCount);
        clearBtn    = findViewById(R.id.clearLogsBtn);

        loadLogs();

        clearBtn.setOnClickListener(v ->
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Clear History")
                        .setMessage("Delete all emergency log history?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            dbHelper.clearEmergencyLogs();
                            loadLogs();
                            Toast.makeText(this, "Log history cleared.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    private void loadLogs() {
        ArrayList<String> logs = dbHelper.getEmergencyLogs();
        if (logs.isEmpty()) {
            logCount.setText("No emergency alerts sent yet.");
        } else {
            logCount.setText(logs.size() + " emergency alert" +
                    (logs.size() > 1 ? "s" : "") + " recorded.");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, logs);
        logListView.setAdapter(adapter);
    }
}