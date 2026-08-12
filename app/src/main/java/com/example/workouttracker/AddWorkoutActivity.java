package com.example.workouttracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddWorkoutActivity extends AppCompatActivity {
    //private --> nur Klasse kann auf VAriablen zugreifen
    //Eingabefeld
    private EditText editTextType;
    private EditText editTextDate;
    private EditText editTextDuration;
    private EditText editTextDistance;
    private EditText editTextNotes;

    private Button buttonSaveWorkout;
    //Fortschrittsanzeige / wird während speichern schon angezeigt
    private ProgressBar progressBar;
    //aktueller Nutzer ermittelt
    private FirebaseAuth firebaseAuth;
    private DatabaseReference workoutsReference;

    private final Calendar selectedDate = Calendar.getInstance();

    //überschreibt eigentliche Methode, initialisiert Benutzeroberfläche
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        String databaseUrl =
                "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

        workoutsReference = FirebaseDatabase
                .getInstance(databaseUrl)
                .getReference("users")
                .child(currentUser.getUid())
                .child("workouts");

        editTextType = findViewById(R.id.editTextWorkoutType);
        editTextDate = findViewById(R.id.editTextWorkoutDate);
        editTextDuration =
                findViewById(R.id.editTextWorkoutDuration);
        editTextDistance =
                findViewById(R.id.editTextWorkoutDistance);
        editTextNotes = findViewById(R.id.editTextWorkoutNotes);

        buttonSaveWorkout =
                findViewById(R.id.buttonSaveWorkout);
        progressBar =
                findViewById(R.id.progressBarSaveWorkout);

        updateDateField();
        //öffnet Date Picker wenn angeklickt wird
        //öffnet Kalender
        editTextDate.setOnClickListener(view ->
                showDatePicker()
        );

        buttonSaveWorkout.setOnClickListener(view ->
                saveWorkout()
        );
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (datePicker, year, month, dayOfMonth) -> {
                    selectedDate.set(
                            Calendar.YEAR,
                            year
                    );

                    selectedDate.set(
                            Calendar.MONTH,
                            month
                    );

                    selectedDate.set(
                            Calendar.DAY_OF_MONTH,
                            dayOfMonth
                    );

                    updateDateField();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void updateDateField() {
        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd.MM.yyyy",
                        Locale.GERMANY
                );

        editTextDate.setText(
                formatter.format(selectedDate.getTime())
        );
    }

    private void saveWorkout() {
        String type = editTextType
                .getText()
                //Wandelt Text in String um
                .toString()
                //entfernt Leerzeichen am Ende oder Anfang
                .trim();

        String date = editTextDate
                .getText()
                .toString()
                .trim();

        String durationText = editTextDuration
                .getText()
                .toString()
                .trim();

        String distanceText = editTextDistance
                .getText()
                .toString()
                .trim()
                .replace(",", ".");

        String notes = editTextNotes
                .getText()
                .toString()
                .trim();

        if (TextUtils.isEmpty(type)) {
            editTextType.setError(
                    "Bitte eine Workout-Art eingeben."
            );
            editTextType.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(durationText)) {
            editTextDuration.setError(
                    "Bitte die Dauer eingeben."
            );
            editTextDuration.requestFocus();
            return;
        }

        int duration;

        try {
            duration = Integer.parseInt(durationText);
        } catch (NumberFormatException exception) {
            editTextDuration.setError(
                    "Bitte eine gültige Dauer eingeben."
            );
            editTextDuration.requestFocus();
            return;
        }

        if (duration <= 0) {
            editTextDuration.setError(
                    "Die Dauer muss größer als 0 sein."
            );
            editTextDuration.requestFocus();
            return;
        }

        double distance = 0;

        if (!TextUtils.isEmpty(distanceText)) {
            try {
                distance = Double.parseDouble(distanceText);
            } catch (NumberFormatException exception) {
                editTextDistance.setError(
                        "Bitte eine gültige Distanz eingeben."
                );
                editTextDistance.requestFocus();
                return;
            }

            if (distance < 0) {
                editTextDistance.setError(
                        "Die Distanz darf nicht negativ sein."
                );
                editTextDistance.requestFocus();
                return;
            }
        }

        String workoutId = workoutsReference.push().getKey();

        if (workoutId == null) {
            Toast.makeText(
                    this,
                    "Workout-ID konnte nicht erstellt werden.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        //Konstruktor --> erzeugt neues Objekt der Klasse
        Workout workout = new Workout(
                workoutId,
                type,
                date,
                duration,
                distance,
                notes,
                selectedDate.getTimeInMillis()
        );

        setLoading(true);

        //speichert Workout in Datenbank
        workoutsReference
                .child(workoutId) //jedes WOrkout bekommt ID
                .setValue(workout)
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    //kleine Nachricht unten am Bildschirm
                    Toast.makeText(
                            AddWorkoutActivity.this,
                            "Workout wurde gespeichert.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            AddWorkoutActivity.this,
                            "Speichern fehlgeschlagen: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonSaveWorkout.setEnabled(!loading);
        editTextType.setEnabled(!loading);
        editTextDate.setEnabled(!loading);
        editTextDuration.setEnabled(!loading);
        editTextDistance.setEnabled(!loading);
        editTextNotes.setEnabled(!loading);
    }
}