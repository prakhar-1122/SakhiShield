package com.pakhar.sakhishield.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.pakhar.sakhishield.R;
import com.pakhar.sakhishield.database.DatabaseHelper;

import java.util.ArrayList;

public class ContactActivity extends AppCompatActivity {
    EditText nameInput, phoneInput;
    Button addBtn;
    ListView listView;
    TextView contactCount;
    DatabaseHelper dbHelper;
    ArrayAdapter<String> adapter;
    ArrayList<String> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        nameInput    = findViewById(R.id.nameInput);
        phoneInput   = findViewById(R.id.phoneInput);
        addBtn       = findViewById(R.id.addContactBtn);
        listView     = findViewById(R.id.contactList);
        contactCount = findViewById(R.id.contactCount);
        dbHelper = new DatabaseHelper(this);
        loadContacts();
        addBtn.setOnClickListener(v -> {
            String name  = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() < 10 || !phone.matches("[0-9]+")) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean added = dbHelper.addContact(name, phone, 0);
            if (added) {
                Toast.makeText(this, "Contact Added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This number already exists!", Toast.LENGTH_SHORT).show();
            }
            nameInput.setText("");
            phoneInput.setText("");
            loadContacts();
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = contactList.get(position);
            boolean isPrimary = selected.contains("[PRIMARY]");
            String clean = selected.replace(" [PRIMARY]", "");
            String[] parts = clean.split(" — ");
            String contactName  = parts[0];
            String contactPhone = parts[1];
            String[] options = isPrimary
                    ? new String[]{"Edit", "Remove Primary", "Delete"}
                    : new String[]{"Edit", "Set as Primary", "Delete"};
            new AlertDialog.Builder(this)
                    .setTitle(contactName)
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            nameInput.setText(contactName);
                            phoneInput.setText(contactPhone);
                            addBtn.setText("Update Contact");
                            addBtn.setOnClickListener(v -> {
                                String newName  = nameInput.getText().toString().trim();
                                String newPhone = phoneInput.getText().toString().trim();
                                if (newName.isEmpty() || newPhone.isEmpty()) {
                                    Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (newPhone.length() < 10 || !newPhone.matches("[0-9]+")) {
                                    Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                int priority = isPrimary ? 1 : 0;
                                dbHelper.deleteContact(contactPhone);
                                dbHelper.addContact(newName, newPhone, priority);
                                Toast.makeText(this, "Contact Updated", Toast.LENGTH_SHORT).show();
                                nameInput.setText("");
                                phoneInput.setText("");
                                resetAddButton();
                                loadContacts();
                            });
                        } else if (which == 1) {
                            // Toggle primary
                            if (isPrimary) {
                                dbHelper.updateContactPriority(contactPhone, 0);
                                Toast.makeText(this, contactName + " removed as primary.", Toast.LENGTH_SHORT).show();
                            } else {
                                // Remove existing primary first
                                ArrayList<String> allPhones = dbHelper.getAllPhoneNumbers();
                                for (String p : allPhones) {
                                    dbHelper.updateContactPriority(p, 0);
                                }
                                dbHelper.updateContactPriority(contactPhone, 1);
                                Toast.makeText(this, contactName + " set as primary contact. Will receive call.", Toast.LENGTH_SHORT).show();
                            }
                            loadContacts();
                        } else if (which == 2) {
                            // Delete
                            new AlertDialog.Builder(this)
                                    .setTitle("Delete Contact")
                                    .setMessage("Remove " + contactName + " from emergency contacts?")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        dbHelper.deleteContact(contactPhone);
                                        Toast.makeText(this, contactName + " removed.", Toast.LENGTH_SHORT).show();
                                        loadContacts();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }).show();
        });
    }

    private void loadContacts() {
        contactList = dbHelper.getAllContacts();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, contactList);
        listView.setAdapter(adapter);

        int count = contactList.size();
        if (count == 0) {
            contactCount.setText("No emergency contacts added yet.");
        } else {
            contactCount.setText(count + " emergency contact" +
                    (count > 1 ? "s" : "") +
                    " added. Tap to set primary.");
        }
    }

    private void resetAddButton() {
        addBtn.setText("Add Contact");
        addBtn.setOnClickListener(v -> {
            String name  = nameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (phone.length() < 10 || !phone.matches("[0-9]+")) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean added = dbHelper.addContact(name, phone, 0);
            if (added) {
                Toast.makeText(this, "Contact Added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This number already exists!", Toast.LENGTH_SHORT).show();
            }
            nameInput.setText("");
            phoneInput.setText("");
            loadContacts();
        });
    }
}