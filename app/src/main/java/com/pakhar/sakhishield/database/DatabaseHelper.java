package com.pakhar.sakhishield.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "SafetyDB";
    private static final int DB_VERSION = 5; // bumped from 4
    public static final String TABLE_CONTACT = "contacts";
    public static final String TABLE_LOG = "emergency_log";
    public static final String TABLE_USERS = "users";
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createContacts = "CREATE TABLE contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "phone TEXT UNIQUE, " +
                "priority INTEGER DEFAULT 0)";
        db.execSQL(createContacts);
        String createLog = "CREATE TABLE emergency_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "status TEXT)";
        db.execSQL(createLog);
        String createUsers = "CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "phone TEXT UNIQUE, " +
                "email TEXT, " +
                "password TEXT, " +
                "home_lat REAL DEFAULT 0, " +
                "home_lon REAL DEFAULT 0, " +
                "home_address TEXT DEFAULT '')";
        db.execSQL(createUsers);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            // Safe migration — only add new columns
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN home_lat REAL DEFAULT 0");
                db.execSQL("ALTER TABLE users ADD COLUMN home_lon REAL DEFAULT 0");
                db.execSQL("ALTER TABLE users ADD COLUMN home_address TEXT DEFAULT ''");
            } catch (Exception e) { /* Column may already exist */ }
        }
    }
    public boolean addContact(String name, String phone, int priority) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        values.put("priority", priority);
        long result = db.insertWithOnConflict(
                TABLE_CONTACT, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result != -1;
    }
    public boolean addContact(String name, String phone) {
        return addContact(name, phone, 0);
    }
    public void updateContactPriority(String phone, int priority) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("priority", priority);
        db.update(TABLE_CONTACT, values, "phone = ?", new String[]{phone});
        db.close();
    }
    public ArrayList<String> getAllContacts() {
        ArrayList<String> contacts = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name, phone, priority FROM contacts ORDER BY priority DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);
                int priority = cursor.getInt(2);
                String label = priority == 1 ? " [PRIMARY]" : "";
                contacts.add(name + " — " + phone + label);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return contacts;
    }
    public ArrayList<String> getAllPhoneNumbers() {
        ArrayList<String> phones = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT phone FROM contacts ORDER BY priority DESC", null);
        if (cursor.moveToFirst()) {
            do {
                phones.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return phones;
    }
    public String getPrimaryContact() {
        SQLiteDatabase db = this.getReadableDatabase();
        String phone = "";
        Cursor cursor = db.rawQuery(
                "SELECT phone FROM contacts WHERE priority = 1 LIMIT 1", null);
        if (cursor.moveToFirst()) {
            phone = cursor.getString(0);
        }
        cursor.close();

        if (phone.isEmpty()) {
            cursor = db.rawQuery("SELECT phone FROM contacts LIMIT 1", null);
            if (cursor.moveToFirst()) phone = cursor.getString(0);
            cursor.close();
        }
        db.close();
        return phone;
    }
    public ArrayList<String> getSecondaryContacts() {
        ArrayList<String> phones = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT phone FROM contacts WHERE priority = 0", null);
        if (cursor.moveToFirst()) {
            do {
                phones.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return phones;
    }
    public void deleteContact(String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACT, "phone = ?", new String[]{phone});
        db.close();
    }

    // Emergency log methods
    public void logEmergency(double latitude, double longitude, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("timestamp", new java.util.Date().toString());
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("status", status);
        db.insert(TABLE_LOG, null, values);
        db.close();
    }
    public ArrayList<String> getEmergencyLogs() {
        ArrayList<String> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT timestamp, latitude, longitude, status " +
                        "FROM emergency_log ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                logs.add(cursor.getString(0) + "\n" +
                        "Location: " + cursor.getDouble(1) +
                        ", " + cursor.getDouble(2) +
                        " | Status: " + cursor.getString(3));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return logs;
    }
    public void clearEmergencyLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOG, null, null);
        db.close();
    }

    // user methods
    public boolean registerUser(String name, String phone, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        values.put("email", email);
        values.put("password", password);
        long result = db.insertWithOnConflict(
                TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result != -1;
    }
    public boolean checkUser(String phone, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM users WHERE phone = ? AND password = ?",
                new String[]{phone, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
    public String[] getUserDetails(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name, phone, email FROM users WHERE phone = ?",
                new String[]{phone});
        String[] details = {"User", phone, ""};
        if (cursor.moveToFirst()) {
            details[0] = cursor.getString(0);
            details[1] = cursor.getString(1);
            details[2] = cursor.getString(2);
        }
        cursor.close();
        db.close();
        return details;
    }
    public void updateUser(String phone, String name, String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        // Only update password if a new one was provided
        if (newPassword != null && !newPassword.isEmpty()) {
            values.put("password", newPassword);
        }

        db.update(TABLE_USERS, values, "phone = ?", new String[]{phone});
        db.close();
    }
    // Save home location for a user
    public void saveHomeLocation(String phone, double lat, double lon, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("home_lat", lat);
        values.put("home_lon", lon);
        values.put("home_address", address);
        db.update(TABLE_USERS, values, "phone = ?", new String[]{phone});
        db.close();
    }
    // Get home location for a user
    public double[] getHomeLocation(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        double[] location = {0, 0};
        Cursor cursor = db.rawQuery(
                "SELECT home_lat, home_lon FROM users WHERE phone = ?",
                new String[]{phone});
        if (cursor.moveToFirst()) {
            location[0] = cursor.getDouble(0);
            location[1] = cursor.getDouble(1);
        }
        cursor.close();
        db.close();
        return location;
    }
    public String getHomeAddress(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        String address = "";
        Cursor cursor = db.rawQuery(
                "SELECT home_address FROM users WHERE phone = ?",
                new String[]{phone});
        if (cursor.moveToFirst()) {
            address = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return address;
    }
}