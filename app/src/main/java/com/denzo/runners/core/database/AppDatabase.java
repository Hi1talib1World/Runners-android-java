package com.denzo.runners.core.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.denzo.runners.data.local.dao.RunningDAO;
import com.denzo.runners.data.local.entities.Runningdata;

@Database(entities = {Runningdata.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract RunningDAO getRunningdataDAO();
}