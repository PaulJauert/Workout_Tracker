package com.example.workouttracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.List;

//erstellt sinnvollen Listeneintrag aus Workout Klasse
public class WorkoutAdapter extends ArrayAdapter<Workout> {

    private final Context context;
    private final List<Workout> workouts;

    public WorkoutAdapter(
            @NonNull Context context,
            @NonNull List<Workout> workouts
    ) {
        super(context, R.layout.item_workout, workouts);

        this.context = context;
        this.workouts = workouts;
    }

    @NonNull
    @Override
    public View getView(
            int position,
            @Nullable View convertView,
            @NonNull ViewGroup parent
    ) {
        View listItemView = convertView;
    // es wird ein Layout erzeugt und muss nicht immer mit neuen Views erstellt werden
        if (listItemView == null) {
            listItemView = LayoutInflater
                    .from(context)
                    .inflate(
                            R.layout.item_workout,
                            parent,
                            false
                    );
        }

        Workout workout = workouts.get(position);

        TextView textViewType =
                listItemView.findViewById(
                        R.id.textViewItemWorkoutType
                );

        TextView textViewDate =
                listItemView.findViewById(
                        R.id.textViewItemWorkoutDate
                );

        TextView textViewDuration =
                listItemView.findViewById(
                        R.id.textViewItemWorkoutDuration
                );

        TextView textViewDistance =
                listItemView.findViewById(
                        R.id.textViewItemWorkoutDistance
                );

        String type = workout.getType();

        if (type == null || type.trim().isEmpty()) {
            type = "Workout";
        }

        String date = workout.getDate();

        if (date == null || date.trim().isEmpty()) {
            date = "Kein Datum";
        }

        textViewType.setText(type);
        textViewDate.setText("Datum: " + date);

        textViewDuration.setText(
                "Dauer: "
                        + workout.getDuration()
                        + " Minuten"
        );

        double distance = workout.getDistance();

        if (distance > 0) {
            DecimalFormat decimalFormat =
                    new DecimalFormat("0.##");

            textViewDistance.setVisibility(View.VISIBLE);

            textViewDistance.setText(
                    "Distanz: "
                            + decimalFormat.format(distance)
                            + " km"
            );
        } else {
            textViewDistance.setVisibility(View.GONE);
        }

        return listItemView;
    }
}