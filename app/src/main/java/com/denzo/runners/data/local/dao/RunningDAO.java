package com.denzo.runners.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.denzo.runners.data.local.entities.Runningdata;

import java.util.List;

@Dao
public interface RunningDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Runningdata runningdata);

    @Update
    void update(Runningdata runningdata);

    @Delete
    void delete(Runningdata runningdata);

    @Query("SELECT * FROM running_table")
    List<Runningdata> getAllRuningdata();

    @Query("DELETE FROM running_table WHERE id = :runningId")
    void deleteById(int runningId);

    @Query("DELETE FROM running_table")
    void deleteall();
}