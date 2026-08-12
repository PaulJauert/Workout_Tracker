package com.example.workouttracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditWorkoutActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_ID = "workoutId";

    private static final String DATABASE_URL =
            "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

    private EditText editTextType;
    private EditText editTextDate;
    private EditText editTextDuration;
    private EditText editTextDistance;
    private EditText editTextNotes;

    private Button buttonUpdateWorkout;
    private ProgressBar progressBar;

    private DatabaseReference workoutReference;
    private ValueEventListener workoutListener;

    private final Calendar selectedDate = Calendar.getInstance();

    private String workoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_workout);

        editTextType =
                findViewById(R.id.editTextEditWorkoutType);

        editTextDate =
                findViewById(R.id.editTextEditWorkoutDate);

        editTextDuration =
                findViewById(R.id.editTextEditWorkoutDuration);

        editTextDistance =
                findViewById(R.id.editTextEditWorkoutDistance);

        editTextNotes =
                findViewById(R.id.editTextEditWorkoutNotes);

        buttonUpdateWorkout =
                findViewById(R.id.buttonUpdateWorkout);

        progressBar =
                findViewById(R.id.progressBarEditWorkout);

        workoutId =
                getIntent().getStringExtra(EXTRA_WORKOUT_ID);

        if (workoutId == null || workoutId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Keine Workout-ID übergeben.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Du bist nicht angemeldet.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        workoutReference = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("users")
                //.child() Methode von Firebase --> untergeordneter Eintrag wird ausgewählt
                .child(currentUser.getUid())
                .child("workouts")
                .child(workoutId);

        editTextDate.setOnClickListener(
                view -> showDatePicker()
        );

        buttonUpdateWorkout.setOnClickListener(
                view -> updateWorkout()
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadWorkout();
    }

    private void loadWorkout() {
        if (workoutReference == null) {
            return;
        }

        if (workoutListener != null) {
            workoutReference.removeEventListener(
                    workoutListener
            );
        }

        setLoading(true);

        workoutListener = new ValueEventListener() {
            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                setLoading(false);

                Workout workout =
                        snapshot.getValue(Workout.class);

                if (workout == null) {
                    Toast.makeText(
                            EditWorkoutActivity.this,
                            "Workout wurde nicht gefunden.",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                    return;
                }

                showWorkout(workout);
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
                setLoading(false);

                Toast.makeText(
                        EditWorkoutActivity.this,
                        "Workout konnte nicht geladen werden: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        workoutReference.addValueEventListener(
                workoutListener
        );
    }

    private void showWorkout(Workout workout) {
        editTextType.setText(
                workout.getType() == null
                        ? ""
                        : workout.getType()
        );

        editTextDate.setText(
                workout.getDate() == null
                        ? ""
                        : workout.getDate()
        );

        editTextDuration.setText(
                String.valueOf(workout.getDuration())
        );

        if (workout.getDistance() > 0) {
            editTextDistance.setText(
                    String.valueOf(workout.getDistance())
            );
        } else {
            editTextDistance.setText("");
        }

        editTextNotes.setText(
                workout.getNotes() == null
                        ? ""
                        : workout.getNotes()
        );

        if (workout.getTimestamp() > 0) {
            selectedDate.setTimeInMillis(
                    workout.getTimestamp()
            );
        } else {
            parseDate(workout.getDate());
        }
    }

    private void parseDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd.MM.yyyy",
                        Locale.GERMANY
                );

        formatter.setLenient(false);

        try {
            selectedDate.setTime(
                    formatter.parse(dateText)
            );
        } catch (ParseException exception) {
            selectedDate.setTimeInMillis(
                    System.currentTimeMillis()
            );
        }
    }

    //öffnet Kalender um Datum auszuwählen
    private void showDatePicker() {
        DatePickerDialog dialog =
                new DatePickerDialog(
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

    private void updateWorkout() {
        String type =
                editTextType
                        .getText()
                        .toString()
                        .trim();

        String date =
                editTextDate
                        .getText()
                        .toString()
                        .trim();

        String durationText =
                editTextDuration
                        .getText()
                        .toString()
                        .trim();

        String distanceText =
                editTextDistance
                        .getText()
                        .toString()
                        .trim()
                        .replace(",", ".");

        String notes =
                editTextNotes
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

        if (TextUtils.isEmpty(date)) {
            editTextDate.setError(
                    "Bitte ein Datum auswählen."
            );

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
            duration =
                    Integer.parseInt(durationText);
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
                distance =
                        Double.parseDouble(distanceText);
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

        Map<String, Object> updates =
                new HashMap<>();

        updates.put("type", type);
        updates.put("date", date);
        updates.put("duration", duration);
        updates.put("distance", distance);
        updates.put("notes", notes);
        updates.put(
                "timestamp",
                selectedDate.getTimeInMillis()
        );

        setLoading(true);


        //Aktualisert Meldung
        workoutReference
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    Toast.makeText(
                            EditWorkoutActivity.this,
                            "Workout wurde aktualisiert.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            EditWorkoutActivity.this,
                            "Aktualisieren fehlgeschlagen: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonUpdateWorkout.setEnabled(!loading);
        editTextType.setEnabled(!loading);
        editTextDate.setEnabled(!loading);
        editTextDuration.setEnabled(!loading);
        editTextDistance.setEnabled(!loading);
        editTextNotes.setEnabled(!loading);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (workoutReference != null
                && workoutListener != null) {

            workoutReference.removeEventListener(
                    workoutListener
            );

            workoutListener = null;
        }
    }
}