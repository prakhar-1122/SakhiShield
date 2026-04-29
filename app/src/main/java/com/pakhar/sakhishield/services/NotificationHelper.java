package com.pakhar.sakhishield.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.activities.EmergencyLogActivity;
public class NotificationHelper {
    private static final String CHANNEL_EMERGENCY = "channel_emergency";
    private static final String CHANNEL_SERVICE   = "channel_service";
    private static final String CHANNEL_ALERT     = "channel_alert";

    public static void createChannels(Context context) {
        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        // Emergency alert channel — high priority
        NotificationChannel emergencyChannel = new NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH);
        emergencyChannel.setDescription("Alerts when panic button is triggered");
        emergencyChannel.enableVibration(true);
        emergencyChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
        manager.createNotificationChannel(emergencyChannel);

        // Service running channel — low priority
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_SERVICE,
                "Safety Service",
                NotificationManager.IMPORTANCE_LOW);
        serviceChannel.setDescription("Keeps Sakhi Shield running in background");
        manager.createNotificationChannel(serviceChannel);

        // General alert channel
        NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_ALERT,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        alertChannel.setDescription("General safety notifications");
        manager.createNotificationChannel(alertChannel);
    }

    // Show notification when emergency SMS is sent
    public static void showEmergencySentNotification(Context context,
                                                     double lat, double lon) {
        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        // Tap notification → open log history
        Intent intent = new Intent(context, EmergencyLogActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Emergency Alert Sent!")
                        .setContentText("SMS and call triggered. Location shared.")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Emergency SMS sent to all contacts.\n" +
                                        "Location: " + lat + ", " + lon + "\n" +
                                        "Tap to view alert history."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 500, 200, 500});

        manager.notify(100, builder.build());
    }

    // Show persistent service notification
    public static android.app.Notification buildServiceNotification(
            Context context, PendingIntent pendingIntent) {
        return new NotificationCompat.Builder(context, CHANNEL_SERVICE)
                .setContentTitle("Sakhi Shield Active")
                .setContentText("Your safety shield is on. Tap to open.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // Show location update notification
    public static void showLocationUpdateNotification(Context context,
                                                      double lat, double lon) {
        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ALERT)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Location Tracked")
                        .setContentText("Current: " + lat + ", " + lon)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        manager.notify(101, builder.build());
    }
}