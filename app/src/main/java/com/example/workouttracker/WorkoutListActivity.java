package com.example.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class WorkoutListActivity extends AppCompatActivity {

    private static final String DATABASE_URL =
            "https://workout-tracker-6749d-default-rtdb.europe-west1.firebasedatabase.app/";

    private static final String FILTER_ALL = "Alle";

    private ListView listViewWorkouts;
    private TextView textViewEmptyWorkoutList;
    private TextView textViewWorkoutResultCount;
    private ProgressBar progressBarWorkoutList;

    private SearchView searchViewWorkouts;
    private Spinner spinnerWorkoutTypeFilter;
    private Spinner spinnerWorkoutSort;

    /*
     * Enthält alle Workouts aus Firebase.
     */
    private final List<Workout> allWorkouts =
            new ArrayList<>();

    /*
     * Enthält nur die aktuell sichtbaren Workouts.
     * Diese Liste wird an den Adapter übergeben.
     */
    private final List<Workout> displayedWorkouts =
            new ArrayList<>();

    private WorkoutAdapter workoutAdapter;

    private DatabaseReference workoutsReference;
    private ValueEventListener workoutsListener;

    private String currentSearchQuery = "";
    private String currentTypeFilter = FILTER_ALL;
    private int currentSortPosition = 0;

    private boolean updatingFilterSpinner = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        initializeViews();

        FirebaseUser currentUser =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

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

        workoutAdapter = new WorkoutAdapter(
                this,
                displayedWorkouts
        );

        listViewWorkouts.setAdapter(workoutAdapter);

        initializeSortSpinner();
        initializeSearch();
        initializeTypeFilter();
        initializeWorkoutClick();
    }

    private void initializeViews() {
        listViewWorkouts =
                findViewById(R.id.listViewWorkouts);

        textViewEmptyWorkoutList =
                findViewById(R.id.textViewEmptyWorkoutList);

        textViewWorkoutResultCount =
                findViewById(R.id.textViewWorkoutResultCount);

        progressBarWorkoutList =
                findViewById(R.id.progressBarWorkoutList);

        searchViewWorkouts =
                findViewById(R.id.searchViewWorkouts);

        spinnerWorkoutTypeFilter =
                findViewById(R.id.spinnerWorkoutTypeFilter);

        spinnerWorkoutSort =
                findViewById(R.id.spinnerWorkoutSort);
    }

    private void initializeSearch() {
        searchViewWorkouts.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {
                        currentSearchQuery =
                                query == null
                                        ? ""
                                        : query.trim();

                        applyFiltersAndSorting();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        currentSearchQuery =
                                newText == null
                                        ? ""
                                        : newText.trim();

                        applyFiltersAndSorting();
                        return true;
                    }
                }
        );
    }

    private void initializeTypeFilter() {
        List<String> initialTypes =
                new ArrayList<>();

        initialTypes.add(FILTER_ALL);

        ArrayAdapter<String> filterAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        initialTypes
                );

        filterAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerWorkoutTypeFilter.setAdapter(
                filterAdapter
        );

        spinnerWorkoutTypeFilter.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        if (updatingFilterSpinner) {
                            return;
                        }

                        Object selectedItem =
                                parent.getItemAtPosition(
                                        position
                                );

                        currentTypeFilter =
                                selectedItem == null
                                        ? FILTER_ALL
                                        : selectedItem.toString();

                        applyFiltersAndSorting();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                        currentTypeFilter = FILTER_ALL;
                        applyFiltersAndSorting();
                    }
                }
        );
    }

    private void initializeSortSpinner() {
        String[] sortingOptions = {
                "Neueste zuerst",
                "Älteste zuerst",
                "Längste Dauer zuerst",
                "Kürzeste Dauer zuerst",
                "Größte Distanz zuerst",
                "Workout-Art A–Z"
        };

        ArrayAdapter<String> sortAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sortingOptions
                );

        sortAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerWorkoutSort.setAdapter(sortAdapter);

        spinnerWorkoutSort.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        currentSortPosition = position;
                        applyFiltersAndSorting();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                        currentSortPosition = 0;
                        applyFiltersAndSorting();
                    }
                }
        );
    }

    private void initializeWorkoutClick() {
        listViewWorkouts.setOnItemClickListener(
                (parent, view, position, id) -> {
                    Workout selectedWorkout =
                            displayedWorkouts.get(position);

                    String workoutId =
                            selectedWorkout.getId();

                    if (workoutId == null
                            || workoutId.trim().isEmpty()) {

                        Toast.makeText(
                                WorkoutListActivity.this,
                                "Workout-ID fehlt.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    Intent intent = new Intent(
                            WorkoutListActivity.this,
                            WorkoutDetailsActivity.class
                    );

                    intent.putExtra(
                            WorkoutDetailsActivity.EXTRA_WORKOUT_ID,
                            workoutId
                    );

                    startActivity(intent);
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadWorkouts();
    }

    private void loadWorkouts() {
        if (workoutsReference == null) {
            return;
        }

        if (workoutsListener != null) {
            workoutsReference.removeEventListener(
                    workoutsListener
            );
        }

        progressBarWorkoutList.setVisibility(View.VISIBLE);
        textViewEmptyWorkoutList.setVisibility(View.GONE);
        listViewWorkouts.setVisibility(View.GONE);

        workoutsListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {
                allWorkouts.clear();

                for (DataSnapshot workoutSnapshot
                        : snapshot.getChildren()) {

                    Workout workout =
                            workoutSnapshot.getValue(
                                    Workout.class
                            );

                    if (workout == null) {
                        continue;
                    }

                    if (workout.getId() == null
                            || workout.getId().trim().isEmpty()) {

                        workout.setId(
                                workoutSnapshot.getKey()
                        );
                    }

                    allWorkouts.add(workout);
                }

                progressBarWorkoutList.setVisibility(
                        View.GONE
                );

                rebuildTypeFilter();
                applyFiltersAndSorting();
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError error
            ) {
                progressBarWorkoutList.setVisibility(
                        View.GONE
                );

                Toast.makeText(
                        WorkoutListActivity.this,
                        "Workouts konnten nicht geladen werden: "
                                + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        workoutsReference.addValueEventListener(
                workoutsListener
        );
    }

    private void rebuildTypeFilter() {
        Set<String> uniqueTypes =
                new TreeSet<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        for (Workout workout : allWorkouts) {
            String type = workout.getType();

            if (type != null
                    && !type.trim().isEmpty()) {

                uniqueTypes.add(type.trim());
            }
        }

        List<String> filterOptions =
                new ArrayList<>();

        filterOptions.add(FILTER_ALL);
        filterOptions.addAll(uniqueTypes);

        updatingFilterSpinner = true;

        ArrayAdapter<String> filterAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        filterOptions
                );

        filterAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerWorkoutTypeFilter.setAdapter(
                filterAdapter
        );

        int selectedPosition =
                filterOptions.indexOf(currentTypeFilter);

        if (selectedPosition < 0) {
            currentTypeFilter = FILTER_ALL;
            selectedPosition = 0;
        }

        spinnerWorkoutTypeFilter.setSelection(
                selectedPosition
        );

        updatingFilterSpinner = false;
    }

    private void applyFiltersAndSorting() {
        displayedWorkouts.clear();

        for (Workout workout : allWorkouts) {
            if (!matchesTypeFilter(workout)) {
                continue;
            }

            if (!matchesSearchQuery(workout)) {
                continue;
            }

            displayedWorkouts.add(workout);
        }

        sortDisplayedWorkouts();

        if (workoutAdapter != null) {
            workoutAdapter.notifyDataSetChanged();
        }

        updateEmptyState();
        updateResultCount();
    }

    private boolean matchesTypeFilter(
            Workout workout
    ) {
        if (FILTER_ALL.equals(currentTypeFilter)) {
            return true;
        }

        String type = workout.getType();

        if (type == null) {
            return false;
        }

        return type.trim().equalsIgnoreCase(
                currentTypeFilter
        );
    }

    private boolean matchesSearchQuery(
            Workout workout
    ) {
        if (currentSearchQuery.isEmpty()) {
            return true;
        }

        String normalizedQuery =
                currentSearchQuery.toLowerCase(
                        Locale.GERMANY
                );

        String type = normalizeText(
                workout.getType()
        );

        String date = normalizeText(
                workout.getDate()
        );

        String notes = normalizeText(
                workout.getNotes()
        );

        String duration =
                String.valueOf(workout.getDuration());

        String distance =
                String.valueOf(workout.getDistance());

        return type.contains(normalizedQuery)
                || date.contains(normalizedQuery)
                || notes.contains(normalizedQuery)
                || duration.contains(normalizedQuery)
                || distance.contains(normalizedQuery);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.GERMANY);
    }

    private void sortDisplayedWorkouts() {
        Comparator<Workout> comparator;

        switch (currentSortPosition) {
            case 1:
                comparator = Comparator.comparingLong(
                        Workout::getTimestamp
                );
                break;

            case 2:
                comparator = (
                        first,
                        second
                ) -> Integer.compare(
                        second.getDuration(),
                        first.getDuration()
                );
                break;

            case 3:
                comparator = Comparator.comparingInt(
                        Workout::getDuration
                );
                break;

            case 4:
                comparator = (
                        first,
                        second
                ) -> Double.compare(
                        second.getDistance(),
                        first.getDistance()
                );
                break;

            case 5:
                comparator = (
                        first,
                        second
                ) -> normalizeText(
                        first.getType()
                ).compareTo(
                        normalizeText(
                                second.getType()
                        )
                );
                break;

            case 0:
            default:
                comparator = (
                        first,
                        second
                ) -> Long.compare(
                        second.getTimestamp(),
                        first.getTimestamp()
                );
                break;
        }

        Collections.sort(
                displayedWorkouts,
                comparator
        );
    }

    private void updateEmptyState() {
        if (displayedWorkouts.isEmpty()) {
            listViewWorkouts.setVisibility(View.GONE);
            textViewEmptyWorkoutList.setVisibility(
                    View.VISIBLE
            );

            if (allWorkouts.isEmpty()) {
                textViewEmptyWorkoutList.setText(
                        "Noch keine Workouts gespeichert."
                );
            } else {
                textViewEmptyWorkoutList.setText(
                        "Keine passenden Workouts gefunden."
                );
            }
        } else {
            listViewWorkouts.setVisibility(View.VISIBLE);
            textViewEmptyWorkoutList.setVisibility(
                    View.GONE
            );
        }
    }

    private void updateResultCount() {
        int count = displayedWorkouts.size();

        if (count == 1) {
            textViewWorkoutResultCount.setText(
                    "1 Workout"
            );
        } else {
            textViewWorkoutResultCount.setText(
                    count + " Workouts"
            );
        }
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