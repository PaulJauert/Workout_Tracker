package com.example.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewForgotPassword;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        editTextEmail =
                findViewById(R.id.editTextLoginEmail);

        editTextPassword =
                findViewById(R.id.editTextLoginPassword);

        buttonLogin =
                findViewById(R.id.buttonPerformLogin);

        textViewForgotPassword =
                findViewById(R.id.textViewForgotPassword);

        progressBar =
                findViewById(R.id.progressBarLogin);

        buttonLogin.setOnClickListener(
                view -> loginUser()
        );

        textViewForgotPassword.setOnClickListener(
                view -> showPasswordResetDialog()
        );
    }

    private void loginUser() {
        String email = editTextEmail
                .getText()
                .toString()
                .trim();

        String password = editTextPassword
                .getText()
                .toString();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(
                    "Bitte E-Mail-Adresse eingeben."
            );

            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(
                    "Bitte Passwort eingeben."
            );

            editTextPassword.requestFocus();
            return;
        }

        setLoading(true);

        //LOGIN Versuch
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Login erfolgreich.",
                                Toast.LENGTH_SHORT
                        ).show();

                        openMainActivity();
                    } else {
                        Toast.makeText(
                                LoginActivity.this,
                                "E-Mail-Adresse oder Passwort ist falsch.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void showPasswordResetDialog() {
        EditText editTextResetEmail =
                new EditText(this);

        editTextResetEmail.setHint("E-Mail-Adresse");
        editTextResetEmail.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        String existingEmail = editTextEmail
                .getText()
                .toString()
                .trim();

        if (!existingEmail.isEmpty()) {
            editTextResetEmail.setText(existingEmail);
        }

        //wandelt in int um um ganze zahl zu speichern
        //padding sorgt dafür dass das Element nicht am Rand liegt
        int padding =
                (int) (24 * getResources()
                        .getDisplayMetrics()
                        .density);

        editTextResetEmail.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Passwort zurücksetzen")
                        .setMessage(
                                "Gib die E-Mail-Adresse deines Kontos ein."
                        )
                        .setView(editTextResetEmail)
                        .setNegativeButton(
                                "Abbrechen",
                                null
                        )
                        .setPositiveButton(
                                "E-Mail senden",
                                null
                        )
                        .create();

        dialog.setOnShowListener(unused -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(view -> {
                String email = editTextResetEmail
                        .getText()
                        .toString()
                        .trim();

                if (TextUtils.isEmpty(email)) {
                    editTextResetEmail.setError(
                            "Bitte E-Mail-Adresse eingeben."
                    );

                    editTextResetEmail.requestFocus();
                    return;
                }

                dialog.dismiss();
                sendPasswordResetEmail(email);
            });
        });

        dialog.show();
    }

    private void sendPasswordResetEmail(String email) {
        setLoading(true);
        // hier wird mail von firebase versendet
        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Die E-Mail zum Zurücksetzen des Passworts wurde versendet.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        String message =
                                "Die E-Mail konnte nicht gesendet werden.";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {

                            message =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openMainActivity() {
        Intent intent = new Intent(
                LoginActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonLogin.setEnabled(!loading);
        textViewForgotPassword.setEnabled(!loading);
        editTextEmail.setEnabled(!loading);
        editTextPassword.setEnabled(!loading);
    }
}