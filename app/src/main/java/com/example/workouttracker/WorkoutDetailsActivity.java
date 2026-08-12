package com.example.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
// AppCompatActivity ist eine Basisklasse und stellt z.B. onCreate() zur verfügung
public class WorkoutDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_WORKOUT_ID = "workoutId";

    private static final String DATABASE_URL =
            "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView textViewType;
    private TextView textViewDate;
    private TextView textViewDuration;
    private TextView textViewDistance;
    private TextView textViewNotes;

    private Button buttonEditWorkout;
    private Button buttonDeleteWorkout;
    private ProgressBar progressBar;

    private DatabaseReference workoutReference;
    private ValueEventListener workoutListener;

    private String workoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_details);

        textViewType =
                findViewById(R.id.textViewDetailsType);

        textViewDate =
                findViewById(R.id.textViewDetailsDate);

        textViewDuration =
                findViewById(R.id.textViewDetailsDuration);

        textViewDistance =
                findViewById(R.id.textViewDetailsDistance);

        textViewNotes =
                findViewById(R.id.textViewDetailsNotes);

        buttonEditWorkout =
                findViewById(R.id.buttonEditWorkout);

        buttonDeleteWorkout =
                findViewById(R.id.buttonDeleteWorkout);

        progressBar =
                findViewById(R.id.progressBarWorkoutDetails);

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
                .child(currentUser.getUid())
                .child("workouts")
                .child(workoutId);

        buttonEditWorkout.setOnClickListener(
                view -> openEditWorkoutActivity()
        );

        buttonDeleteWorkout.setOnClickListener(
                view -> showDeleteConfirmation()
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

        progressBar.setVisibility(View.VISIBLE);

        workoutListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                progressBar.setVisibility(View.GONE);

                Workout workout =
                        snapshot.getValue(Workout.class);

                if (workout == null) {
                    Toast.makeText(
                            WorkoutDetailsActivity.this,
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
                progressBar.setVisibility(View.GONE);

                Toast.makeText(
                        WorkoutDetailsActivity.this,
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
        String type = workout.getType();

        if (type == null || type.trim().isEmpty()) {
            type = "Workout";
        }

        String date = workout.getDate();

        if (date == null || date.trim().isEmpty()) {
            date = "Kein Datum";
        }

        String notes = workout.getNotes();

        if (notes == null || notes.trim().isEmpty()) {
            notes = "Keine Notizen";
        }

        textViewType.setText(type);
        textViewDate.setText("Datum: " + date);

        textViewDuration.setText(
                "Dauer: "
                        + workout.getDuration()
                        + " Minuten"
        );

        if (workout.getDistance() > 0) {
            DecimalFormat decimalFormat =
                    new DecimalFormat("0.##");

            textViewDistance.setText(
                    "Distanz: "
                            + decimalFormat.format(
                            workout.getDistance()
                    )
                            + " km"
            );
        } else {
            textViewDistance.setText(
                    "Distanz: Keine Angabe"
            );
        }

        textViewNotes.setText(
                "Notizen: " + notes
        );
    }

    private void openEditWorkoutActivity() {
        Intent intent = new Intent(
                WorkoutDetailsActivity.this,
                EditWorkoutActivity.class
        );

        intent.putExtra(
                EditWorkoutActivity.EXTRA_WORKOUT_ID,
                workoutId
        );

        startActivity(intent);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Workout löschen")
                .setMessage(
                        "Möchtest du dieses Workout wirklich löschen?"
                )
                .setNegativeButton(
                        "Abbrechen",
                        null
                )
                .setPositiveButton(
                        "Löschen",
                        (dialog, which) -> deleteWorkout()
                )
                .show();
    }

    private void deleteWorkout() {
        if (workoutReference == null) {
            return;
        }

        setLoading(true);

        workoutReference
                .removeValue()
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    Toast.makeText(
                            WorkoutDetailsActivity.this,
                            "Workout wurde gelöscht.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);

                    Toast.makeText(
                            WorkoutDetailsActivity.this,
                            "Löschen fehlgeschlagen: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        buttonEditWorkout.setEnabled(!loading);
        buttonDeleteWorkout.setEnabled(!loading);
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