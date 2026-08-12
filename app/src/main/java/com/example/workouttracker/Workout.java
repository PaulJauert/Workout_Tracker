package com.example.workouttracker;

public class Workout {

    private String id;
    private String type;
    private String date;
    private int duration;
    private double distance;
    private String notes;
    private long timestamp;

    public Workout() {
        // Leerer Konstruktor wird von Firebase benötigt.
    }

    public Workout(
            String id,
            String type,
            String date,
            int duration,
            double distance,
            String notes,
            long timestamp
    ) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.duration = duration;
        this.distance = distance;
        this.notes = notes;
        this.timestamp = timestamp;
    }
    //getter und setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}