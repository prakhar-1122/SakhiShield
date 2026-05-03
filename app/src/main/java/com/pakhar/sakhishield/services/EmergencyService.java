package com.pakhar.sakhishield.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.pakhar.sakhishield.activities.MainActivity;
import com.pakhar.sakhishield.database.DatabaseHelper;

public class EmergencyService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private FusedLocationProviderClient fusedLocationClient; //gps tracking engine
    private LocationCallback locationCallback; //location updates

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifIntent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(NOTIFICATION_ID,
                NotificationHelper.buildServiceNotification(this, pendingIntent)); //keeps continuous protection active
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationTracking(); //prepares callback system
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLocationUpdates();  // start gps updates
        return START_STICKY; // ensures long-term reliability of emergency monitoring.

    }
    private void setupLocationTracking() {
        locationCallback = new LocationCallback() { //Anonymous callback class
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    SharedPreferences prefs = getSharedPreferences(   // Save last known location to SharedPreferences
                            "SakhiShieldPrefs", MODE_PRIVATE);
                    prefs.edit() //Saves latest location continuously
                            .putString("lastLat", String.valueOf(lat))
                            .putString("lastLon", String.valueOf(lon))
                            .apply();
                }
            }
        };
    }
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
            return;
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60000) // every 60 seconds
                .setMinUpdateIntervalMillis(30000) // minimum 30 seconds
                .build();
        fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}