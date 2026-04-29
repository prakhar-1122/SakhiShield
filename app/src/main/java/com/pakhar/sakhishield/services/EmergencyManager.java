package com.pakhar.sakhishield.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.pakhar.sakhishield.database.DatabaseHelper;

import java.util.ArrayList;

public class EmergencyManager {

    Context context;
    DatabaseHelper dbHelper;

    public EmergencyManager(DatabaseHelper dbHelper, Context context) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public void sendEmergency(double lat, double lon) {
        ArrayList<String> allContacts = dbHelper.getAllPhoneNumbers();

        if (allContacts.isEmpty()) {
            Toast.makeText(context,
                    "No contacts found! Please add emergency contacts.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String message = buildEmergencyMessage(lat, lon);
        sendSmsToAll(allContacts, message);
        NotificationHelper.showEmergencySentNotification(context, lat, lon);
        dbHelper.logEmergency(lat, lon, "SENT");
    }

    public void makeCall() {
        String primaryPhone = dbHelper.getPrimaryContact();

        if (primaryPhone.isEmpty()) {
            Toast.makeText(context,
                    "No primary contact set!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context,
                    "Call permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + primaryPhone));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private String buildEmergencyMessage(double lat, double lon) {
        String mapsLink = "https://maps.google.com/?q=" + lat + "," + lon;
        StringBuilder msg = new StringBuilder();
        msg.append("EMERGENCY ALERT!\n");
        msg.append("I am in danger!\n");
        msg.append("Location: ").append(mapsLink).append("\n");

        SharedPreferences prefs = context.getSharedPreferences(
                "SakhiShieldPrefs", Context.MODE_PRIVATE);
        String userPhone = prefs.getString("userPhone", "");

        if (!userPhone.isEmpty()) {
            double[] homeLocation = dbHelper.getHomeLocation(userPhone);
            String homeAddress = dbHelper.getHomeAddress(userPhone);

            if (homeLocation[0] != 0 && homeLocation[1] != 0) {
                float[] distance = new float[1];
                android.location.Location.distanceBetween(
                        lat, lon,
                        homeLocation[0], homeLocation[1],
                        distance);

                if (distance[0] < 500) {
                    msg.append("Near home location");
                    if (!homeAddress.isEmpty()) {
                        msg.append(": ").append(homeAddress);
                    }
                    msg.append("\n");
                }
            }
        }

        return msg.toString();
    }

    private void sendSmsToAll(ArrayList<String> contacts, String message) {
        // FIX #3: Use context.getSystemService() on Android 12+ (API 31+)
        SmsManager smsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = context.getSystemService(SmsManager.class);
        } else {
            smsManager = SmsManager.getDefault();
        }

        if (smsManager == null) {
            Toast.makeText(context,
                    "SMS not available on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        int sentCount = 0;
        for (String phone : contacts) {
            try {
                ArrayList<String> parts = smsManager.divideMessage(message);
                if (parts.size() == 1) {
                    smsManager.sendTextMessage(phone, null, message, null, null);
                } else {
                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                }
                sentCount++;
            } catch (Exception e) {
                e.printStackTrace();
                // FIX #5: Use Toast instead of AlertDialog — AlertDialog crashes
                // when shown from a non-Activity Context (BadTokenException)
                Toast.makeText(context,
                        "Failed to send SMS to: " + phone, Toast.LENGTH_SHORT).show();
            }
        }

        if (sentCount > 0) {
            Toast.makeText(context,
                    "Emergency SMS sent to " + sentCount + " contact" +
                            (sentCount > 1 ? "s" : "") + "!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context,
                    "SMS sending failed. Please check SIM and permissions.",
                    Toast.LENGTH_LONG).show();
        }
    }
}