package com.denzo.runners;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "running_table")
public class Runningdata {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "calorie")
    private double calorie;

    @ColumnInfo(name = "starttime")
    private String starttime;

    @ColumnInfo(name = "distance")
    public double distance;

    @ColumnInfo(name = "duration_seconds")
    public int durationSeconds;

    @ColumnInfo(name = "steps")
    public int steps;

    @ColumnInfo(name = "avg_pace")
    public double avgPace;

    @ColumnInfo(name = "vo2_max")
    public double vo2Max;

    @ColumnInfo(name = "intensity_score")
    public double intensityScore;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    //-----get and set functions------//
    public int getId() { return id; }
    public String getStarttime() { return starttime; }
    public void setStarttime(String starttime) { this.starttime = starttime; }
    public double getCalorie() { return calorie; }
    public void setCalorie(double calorie) { this.calorie = calorie; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}