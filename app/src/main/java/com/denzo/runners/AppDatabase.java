package com.denzo.runners;


import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Runningdata.class}, version = 2,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract RunningDAO getRunningdataDAO();
}