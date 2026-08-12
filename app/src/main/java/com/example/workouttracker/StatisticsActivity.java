package com.example.workouttracker;

import android.os.Bundle;
import android.view.View;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class StatisticsActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView textViewNoStatistics;
    private TextView textViewTotalWorkouts;
    private TextView textViewTotalDuration;
    private TextView textViewTotalDistance;
    private TextView textViewAverageDuration;
    private TextView textViewFavoriteWorkoutType;
    private TextView textViewCurrentStreak;
    private TextView textViewLongestStreak;
    private ProgressBar progressBarStatistics;

    private DatabaseReference workoutsReference;
    private ValueEventListener workoutsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initializeViews();

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

        workoutsReference = FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("users")
                .child(currentUser.getUid())
                .child("workouts");
    }

    private void initializeViews() {
        textViewNoStatistics =
                findViewById(R.id.textViewNoStatistics);

        textViewTotalWorkouts =
                findViewById(R.id.textViewTotalWorkouts);

        textViewTotalDuration =
                findViewById(R.id.textViewTotalDuration);

        textViewTotalDistance =
                findViewById(R.id.textViewTotalDistance);

        textViewAverageDuration =
                findViewById(R.id.textViewAverageDuration);

        textViewFavoriteWorkoutType =
                findViewById(R.id.textViewFavoriteWorkoutType);

        textViewCurrentStreak =
                findViewById(R.id.textViewCurrentStreak);

        textViewLongestStreak =
                findViewById(R.id.textViewLongestStreak);

        progressBarStatistics =
                findViewById(R.id.progressBarStatistics);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadStatistics();
    }

    private void loadStatistics() {
        if (workoutsReference == null) {
            return;
        }

        if (workoutsListener != null) {
            workoutsReference.removeEventListener(workoutsListener);
        }

        progressBarStatistics.setVisibility(View.VISIBLE);
        textViewNoStatistics.setVisibility(View.GONE);

        workoutsListener = new ValueEventListener() {
            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                progressBarStatistics.setVisibility(View.GONE);

                long workoutCount = 0;
                int totalDuration = 0;
                double totalDistance = 0.0;

                Map<String, Integer> workoutTypeCounts =
                        new HashMap<>();

                Set<Long> workoutDays =
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

                    addWorkoutType(
                            workoutTypeCounts,
                            workout.getType()
                    );

                    if (workout.getTimestamp() > 0) {
                        workoutDays.add(
                                normalizeToDay(
                                        workout.getTimestamp()
                                )
                        );
                    }
                }

                if (workoutCount == 0) {
                    showEmptyStatistics();
                    return;
                }

                double averageDuration =
                        (double) totalDuration / workoutCount;

                int currentStreak =
                        calculateCurrentStreak(workoutDays);

                int longestStreak =
                        calculateLongestStreak(workoutDays);

                String favoriteWorkoutType =
                        findFavoriteWorkoutType(
                                workoutTypeCounts
                        );

                showStatistics(
                        workoutCount,
                        totalDuration,
                        totalDistance,
                        averageDuration,
                        favoriteWorkoutType,
                        currentStreak,
                        longestStreak
                );
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
                progressBarStatistics.setVisibility(View.GONE);

                Toast.makeText(
                        StatisticsActivity.this,
                        "Statistiken konnten nicht geladen werden: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        workoutsReference.addValueEventListener(
                workoutsListener
        );
    }

    private void addWorkoutType(
            Map<String, Integer> workoutTypeCounts,
            String type
    ) {
        if (type == null || type.trim().isEmpty()) {
            type = "Workout";
        }

        type = type.trim();

        Integer previousCount =
                workoutTypeCounts.get(type);

        if (previousCount == null) {
            previousCount = 0;
        }

        workoutTypeCounts.put(
                type,
                previousCount + 1
        );
    }

    private String findFavoriteWorkoutType(
            Map<String, Integer> workoutTypeCounts
    ) {
        String favoriteType = "Keine";
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry
                : workoutTypeCounts.entrySet()) {

            if (entry.getValue() > highestCount) {
                highestCount = entry.getValue();
                favoriteType = entry.getKey();
            }
        }

        return favoriteType;
    }

    private long normalizeToDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    private int calculateCurrentStreak(
            Set<Long> workoutDays
    ) {
        if (workoutDays.isEmpty()) {
            return 0;
        }

        long today =
                normalizeToDay(System.currentTimeMillis());

        long yesterday =
                today - TimeUnit.DAYS.toMillis(1);

        long startDay;

        if (workoutDays.contains(today)) {
            startDay = today;
        } else if (workoutDays.contains(yesterday)) {
            startDay = yesterday;
        } else {
            return 0;
        }

        int streak = 0;
        long currentDay = startDay;

        while (workoutDays.contains(currentDay)) {
            streak++;
            currentDay -= TimeUnit.DAYS.toMillis(1);
        }

        return streak;
    }

    private int calculateLongestStreak(
            Set<Long> workoutDays
    ) {
        if (workoutDays.isEmpty()) {
            return 0;
        }
        //speichert und sortiert
        TreeSet<Long> sortedDays =
                new TreeSet<>(workoutDays);

        int longestStreak = 1;
        int currentStreak = 1;

        Long previousDay = null;

        for (Long currentDay : sortedDays) {
            if (previousDay != null) {
                long difference =
                        currentDay - previousDay;

                if (difference
                        == TimeUnit.DAYS.toMillis(1)) {

                    currentStreak++;
                } else {
                    currentStreak = 1;
                }

                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
            }

            previousDay = currentDay;
        }

        return longestStreak;
    }

    private void showEmptyStatistics() {
        textViewNoStatistics.setVisibility(View.VISIBLE);

        textViewTotalWorkouts.setText(
                "Workouts insgesamt: 0"
        );

        textViewTotalDuration.setText(
                "Gesamte Trainingszeit: 0 Minuten"
        );

        textViewTotalDistance.setText(
                "Gesamtdistanz: 0 km"
        );

        textViewAverageDuration.setText(
                "Durchschnittliche Dauer: 0 Minuten"
        );

        textViewFavoriteWorkoutType.setText(
                "Häufigste Workout-Art: Keine"
        );

        textViewCurrentStreak.setText(
                "Aktuelle Streak: 0 Tage"
        );

        textViewLongestStreak.setText(
                "Längste Streak: 0 Tage"
        );
    }

    private void showStatistics(
            long workoutCount,
            int totalDuration,
            double totalDistance,
            double averageDuration,
            String favoriteWorkoutType,
            int currentStreak,
            int longestStreak
    ) {
        textViewNoStatistics.setVisibility(View.GONE);

        DecimalFormat decimalFormat =
                new DecimalFormat("0.##");

        textViewTotalWorkouts.setText(
                "Workouts insgesamt: " + workoutCount
        );

        textViewTotalDuration.setText(
                "Gesamte Trainingszeit: "
                        + totalDuration
                        + " Minuten"
        );

        textViewTotalDistance.setText(
                "Gesamtdistanz: "
                        + decimalFormat.format(totalDistance)
                        + " km"
        );

        textViewAverageDuration.setText(
                "Durchschnittliche Dauer: "
                        + decimalFormat.format(averageDuration)
                        + " Minuten"
        );

        textViewFavoriteWorkoutType.setText(
                "Häufigste Workout-Art: "
                        + favoriteWorkoutType
        );

        textViewCurrentStreak.setText(
                "Aktuelle Streak: "
                        + currentStreak
                        + formatDayText(currentStreak)
        );

        textViewLongestStreak.setText(
                "Längste Streak: "
                        + longestStreak
                        + formatDayText(longestStreak)
        );
    }

    private String formatDayText(int numberOfDays) {
        return numberOfDays == 1
                ? " Tag"
                : " Tage";
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
}