package com.example.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextPasswordConfirm;
    private Button buttonCreateAccount;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        // instanz aus Datenbank gewinnen
        firebaseAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextRegisterEmail);
        editTextPassword = findViewById(R.id.editTextRegisterPassword);
        editTextPasswordConfirm =
                findViewById(R.id.editTextRegisterPasswordConfirm);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        progressBar = findViewById(R.id.progressBarRegister);

        buttonCreateAccount.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String passwordConfirm =
                editTextPasswordConfirm.getText().toString();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Bitte E-Mail-Adresse eingeben.");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Bitte Passwort eingeben.");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError(
                    "Das Passwort muss mindestens 6 Zeichen lang sein."
            );
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            editTextPasswordConfirm.setError(
                    "Die Passwörter stimmen nicht überein."
            );
            editTextPasswordConfirm.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                RegisterActivity.this,
                                "Registrierung erfolgreich.",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                RegisterActivity.this,
                                MainActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    } else {
                        String message = "Registrierung fehlgeschlagen.";

                        if (task.getException() != null
                                && task.getException().getMessage() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(
                                RegisterActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonCreateAccount.setEnabled(!loading);
        editTextEmail.setEnabled(!loading);
        editTextPassword.setEnabled(!loading);
        editTextPasswordConfirm.setEnabled(!loading);
    }
}