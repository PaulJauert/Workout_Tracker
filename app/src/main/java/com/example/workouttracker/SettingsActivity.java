package com.example.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView textViewEmail;

    private Button buttonResetPassword;
    private Button buttonLogout;
    private Button buttonDeleteAllWorkouts;
    private Button buttonDeleteAccount;

    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private DatabaseReference userReference;
    private DatabaseReference workoutsReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openStartActivity();
            return;
        }

        String email = currentUser.getEmail();

        if (email == null || email.trim().isEmpty()) {
            textViewEmail.setText("Keine E-Mail-Adresse verfügbar");
        } else {
            textViewEmail.setText(email);
        }

        userReference = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("users")
                .child(currentUser.getUid());

        workoutsReference = userReference.child("workouts");

        buttonResetPassword.setOnClickListener(
                view -> sendPasswordResetEmail()
        );

        buttonLogout.setOnClickListener(
                view -> logout()
        );

        buttonDeleteAllWorkouts.setOnClickListener(
                view -> showDeleteAllWorkoutsConfirmation()
        );

        buttonDeleteAccount.setOnClickListener(
                view -> showDeleteAccountConfirmation()
        );
    }

    private void initializeViews() {
        textViewEmail =
                findViewById(R.id.textViewSettingsEmail);

        buttonResetPassword =
                findViewById(R.id.buttonResetPassword);

        buttonLogout =
                findViewById(R.id.buttonLogoutSettings);

        buttonDeleteAllWorkouts =
                findViewById(R.id.buttonDeleteAllWorkouts);

        buttonDeleteAccount =
                findViewById(R.id.buttonDeleteAccount);

        progressBar =
                findViewById(R.id.progressBarSettings);
    }

    private void sendPasswordResetEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            openStartActivity();
            return;
        }

        String email = user.getEmail();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Keine gültige E-Mail-Adresse gefunden.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        setLoading(true);

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "Eine E-Mail zum Zurücksetzen des Passworts wurde versendet.",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "E-Mail konnte nicht gesendet werden: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void logout() {
        firebaseAuth.signOut();
        openStartActivity();
    }

    private void showDeleteAllWorkoutsConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Alle Workouts löschen")
                .setMessage(
                        "Möchtest du wirklich alle gespeicherten Workouts löschen?"
                )
                .setNegativeButton(
                        "Abbrechen",
                        null
                )
                .setPositiveButton(
                        "Alle löschen",
                        (dialog, which) -> deleteAllWorkouts()
                )
                .show();
    }

    private void deleteAllWorkouts() {
        if (workoutsReference == null) {
            return;
        }

        setLoading(true);

        workoutsReference
                .removeValue()
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "Alle Workouts wurden gelöscht.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "Workouts konnten nicht gelöscht werden: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Konto endgültig löschen")
                .setMessage(
                        "Dabei werden dein Benutzerkonto und alle Workouts dauerhaft gelöscht. Möchtest du fortfahren?"
                )
                .setNegativeButton(
                        "Abbrechen",
                        null
                )
                .setPositiveButton(
                        "Konto löschen",
                        (dialog, which) -> deleteAccountAndData()
                )
                .show();
    }

    private void deleteAccountAndData() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            openStartActivity();
            return;
        }

        setLoading(true);

        /*
         * Zuerst werden die Daten des Benutzers gelöscht.
         * Danach wird das Firebase-Authentication-Konto entfernt.
         */
        userReference
                .removeValue()
                .addOnSuccessListener(unused ->
                        deleteFirebaseUser(user)
                )
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "Benutzerdaten konnten nicht gelöscht werden: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void deleteFirebaseUser(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "Dein Konto wurde gelöscht.",
                            Toast.LENGTH_SHORT
                    ).show();

                    openStartActivity();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            SettingsActivity.this,
                            "Das Konto konnte nicht gelöscht werden. "
                                    + "Bitte melde dich erneut an und versuche es noch einmal.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonResetPassword.setEnabled(!loading);
        buttonLogout.setEnabled(!loading);
        buttonDeleteAllWorkouts.setEnabled(!loading);
        buttonDeleteAccount.setEnabled(!loading);
    }

    private void openStartActivity() {
        Intent intent = new Intent(
                SettingsActivity.this,
                StartActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}