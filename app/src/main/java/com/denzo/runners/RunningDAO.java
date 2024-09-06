package com.denzo.runners;



import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RunningDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(Runningdata runningdata);

    @Update
    public void update(Runningdata runningdata);

    @Delete
    public void delete(Runningdata runningdata);

    @Query("SELECT * FROM running_table")
    public List<Runningdata> getAllRuningdata();

    // @Query can also do delete and updata database
    // UPDATE or DELETE queries can return void or int. If it is an int, the value is the number of rows affected by this query.
    @Query("DELETE FROM running_table WHERE id = :runningId")
    abstract void deleteById(int runningId);// delete row by Id, and since id is the autogenerated pramiray key it has to be Id

    @Query("DELETE FROM running_table")
    abstract void deleteall();


    /* @Query("SELECT * FROM running_table WHERE test = :running_test")
    public List<Runningdata> getRunningInfoBytest(int running_test);*/

}