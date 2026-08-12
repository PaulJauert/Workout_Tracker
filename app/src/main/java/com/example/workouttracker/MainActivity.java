package com.example.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

    private FirebaseAuth firebaseAuth;

    private TextView textViewWelcome;
    private TextView textViewTotalWorkoutsValue;
    private TextView textViewTotalDurationValue;
    private TextView textViewTotalDistanceValue;
    private TextView textViewCurrentStreakValue;
    private TextView textViewLastWorkoutValue;

    private ProgressBar progressBarDashboard;

    private DatabaseReference workoutsReference;
    private ValueEventListener workoutsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        initializeViews();

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openStartActivity();
            return;
        }

        showWelcomeText(currentUser);

        workoutsReference = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("users")
                .child(currentUser.getUid())
                .child("workouts");

        initializeButtons();
    }

    private void initializeViews() {
        textViewWelcome =
                findViewById(R.id.textViewWelcome);

        textViewTotalWorkoutsValue =
                findViewById(R.id.textViewTotalWorkoutsValue);

        textViewTotalDurationValue =
                findViewById(R.id.textViewTotalDurationValue);

        textViewTotalDistanceValue =
                findViewById(R.id.textViewTotalDistanceValue);

        textViewCurrentStreakValue =
                findViewById(R.id.textViewCurrentStreakValue);

        textViewLastWorkoutValue =
                findViewById(R.id.textViewLastWorkoutValue);

        progressBarDashboard =
                findViewById(R.id.progressBarDashboard);
    }

    private void initializeButtons() {
        Button buttonAddWorkout =
                findViewById(R.id.buttonAddWorkout);

        Button buttonShowWorkouts =
                findViewById(R.id.buttonShowWorkouts);

        Button buttonStatistics =
                findViewById(R.id.buttonStatistics);

        Button buttonSettings =
                findViewById(R.id.buttonSettings);

        Button buttonLogout =
                findViewById(R.id.buttonLogout);

        buttonAddWorkout.setOnClickListener(view ->
                startActivity(
                        new Intent(
                                MainActivity.this,
                                AddWorkoutActivity.class
                        )
                )
        );

        buttonShowWorkouts.setOnClickListener(view ->
                startActivity(
                        new Intent(
                                MainActivity.this,
                                WorkoutListActivity.class
                        )
                )
        );

        buttonStatistics.setOnClickListener(view ->
                startActivity(
                        new Intent(
                                MainActivity.this,
                                StatisticsActivity.class
                        )
                )
        );

        buttonSettings.setOnClickListener(view ->
                startActivity(
                        new Intent(
                                MainActivity.this,
                                SettingsActivity.class
                        )
                )
        );

        buttonLogout.setOnClickListener(view -> {
            firebaseAuth.signOut();
            openStartActivity();
        });
    }

    private void showWelcomeText(FirebaseUser user) {
        String email = user.getEmail();

        if (email == null || email.trim().isEmpty()) {
            textViewWelcome.setText("Hallo 👋");
            return;
        }

        String displayName = email;

        int atPosition = email.indexOf("@");

        if (atPosition > 0) {
            displayName = email.substring(0, atPosition);
        }

        if (!displayName.isEmpty()) {
            displayName =
                    displayName.substring(0, 1).toUpperCase(
                            Locale.GERMANY
                    )
                            + displayName.substring(1);
        }

        textViewWelcome.setText(
                "Hallo " + displayName + " 👋"
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            openStartActivity();
            return;
        }

        loadDashboardData();
    }

    private void loadDashboardData() {
        if (workoutsReference == null) {
            return;
        }

        if (workoutsListener != null) {
            workoutsReference.removeEventListener(
                    workoutsListener
            );
        }

        progressBarDashboard.setVisibility(View.VISIBLE);

        workoutsListener = new ValueEventListener() {
            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                progressBarDashboard.setVisibility(View.GONE);

                long workoutCount = 0;
                int totalDuration = 0;
                double totalDistance = 0.0;

                long newestTimestamp = Long.MIN_VALUE;
                String lastWorkoutText =
                        "Noch kein Workout gespeichert";

                Set<String> workoutDays =
                        new HashSet<>();

                for (DataSnapshot workoutSnapshot
                        : snapshot.getChildren()) {

                    Workout workout =
                            workoutSnapshot.getValue(
                                    Workout.class
                            );

                    if (workout == null) {
                        continue;
                    }

                    workoutCount++;
                    totalDuration += workout.getDuration();
                    totalDistance += workout.getDistance();
                    //timestamp ist ein Zeitstempel / Wert an dem etwas festgelegt wurde
                    long timestamp = workout.getTimestamp();

                    if (timestamp > 0) {
                        workoutDays.add(
                                createDayKey(timestamp)
                        );
                    }

                    if (timestamp > newestTimestamp) {
                        newestTimestamp = timestamp;
                        lastWorkoutText =
                                createLastWorkoutText(workout);
                    }
                }

                int currentStreak =
                        calculateCurrentStreak(workoutDays);

                showDashboardValues(
                        workoutCount,
                        totalDuration,
                        totalDistance,
                        currentStreak,
                        lastWorkoutText
                );
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
                progressBarDashboard.setVisibility(View.GONE);

                Toast.makeText(
                        MainActivity.this,
                        "Dashboard konnte nicht geladen werden: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        workoutsReference.addValueEventListener(
                workoutsListener
        );
    }

    private String createLastWorkoutText(Workout workout) {
        String type = workout.getType();

        if (type == null || type.trim().isEmpty()) {
            type = "Workout";
        }

        String date = workout.getDate();

        if (date == null || date.trim().isEmpty()) {
            return type;
        }

        return type + "\n" + date;
    }

    private void showDashboardValues(
            long workoutCount,
            int totalDuration,
            double totalDistance,
            int currentStreak,
            String lastWorkoutText
    ) {
        DecimalFormat decimalFormat =
                new DecimalFormat("0.##");

        textViewTotalWorkoutsValue.setText(
                String.valueOf(workoutCount)
        );

        textViewTotalDurationValue.setText(
                formatDuration(totalDuration)
        );

        textViewTotalDistanceValue.setText(
                decimalFormat.format(totalDistance) + " km"
        );

        textViewCurrentStreakValue.setText(
                currentStreak
                        + (currentStreak == 1
                        ? " Tag"
                        : " Tage")
        );

        textViewLastWorkoutValue.setText(
                lastWorkoutText
        );
    }

    private String formatDuration(int totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " Min.";
        }

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (minutes == 0) {
            return hours + " Std.";
        }

        return hours + " Std. " + minutes + " Min.";
    }

    private int calculateCurrentStreak(
            Set<String> workoutDays
    ) {
        if (workoutDays.isEmpty()) {
            return 0;
        }

        Calendar calendar = Calendar.getInstance();

        String todayKey =
                createDayKey(calendar.getTimeInMillis());

        if (!workoutDays.contains(todayKey)) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            String yesterdayKey =
                    createDayKey(calendar.getTimeInMillis());

            if (!workoutDays.contains(yesterdayKey)) {
                return 0;
            }
        }

        int streak = 0;

        while (workoutDays.contains(
                createDayKey(calendar.getTimeInMillis())
        )) {
            streak++;
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    private String createDayKey(long timestamp) {
        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.GERMANY
                );

        return formatter.format(timestamp);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (workoutsReference != null
                && workoutsListener != null) {

            workoutsReference.removeEventListener(
                    workoutsListener
            );

            workoutsListener = null;
        }
    }

    private void openStartActivity() {
        Intent intent = new Intent(
                MainActivity.this,
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