package com.nyzg.swiftsail.dao;

import androidx.room.Dao;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.nyzg.swiftsail.dbobj.PersonalData;

import java.util.List;

@Dao
public abstract class PersonalDataTable {

    @Query("""
            INSERT INTO `personalDataTable` (appellation,gender,description,bkImage,layoutImage)\s
            VALUES (:appellation,:gender,:description,:bkImage,:layoutImage)
            """)
    public abstract void insertPersonalData(
            String appellation, short gender, String description,
            String bkImage, String layoutImage
    );

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertPersonalData(PersonalData personalData);

    @Query("""
             UPDATE `personalDataTable` SET\s
             appellation=:appellation,gender=:gender,
             description=:description,bkImage=:bkImage,
             layoutImage=:layoutImage
            \s""")
    public abstract void updatePersonalData(
            String appellation, short gender, String description,
            String bkImage, String layoutImage
    );

    @Query("""
            INSERT INTO `personalDataTable` (appellation,gender,description,bkImage,layoutImage,promiseScore,inTimeScore,levelScore,cooperationScore,communicateScore)\s
            VALUES (:appellation,:gender,:description,:bkImage,:layoutImage,:promiseScore,:inTimeScore,:levelScore,:cooperationScore,:communicateScore)
            """)
    public abstract void insertPersonalData(
            String appellation, short gender, String description,
            short promiseScore, short inTimeScore,
            short levelScore, short cooperationScore,
            short communicateScore,
            String bkImage, String layoutImage
    );

    @Query("""
            UPDATE `personalDataTable` SET\s
            appellation=:appellation,gender=:gender,description=:description,
            promiseScore=:promiseScore,inTimeScore=:inTimeScore,
            levelScore=:levelScore,cooperationScore=:cooperationScore,
            communicateScore=:communicateScore,
            bkImage=:bkImage,layoutImage=:layoutImage
            """)
    public abstract void updatePersonalData(
            String appellation, short gender, String description,
            short promiseScore, short inTimeScore,
            short levelScore, short cooperationScore,
            short communicateScore,
            String bkImage, String layoutImage
    );

    @Query("SELECT * FROM `personalDataTable` WHERE userId=:userId")
    abstract public List<PersonalData> selectPersonData(long userId);

    @Transaction
    public void insertDataSafe(
            long userId,
            String appellation, short gender, String description,
            String bkImage, String layoutImage) {
        List<PersonalData> personalDataList = selectPersonData(userId);
        if (personalDataList.isEmpty()) {
            insertPersonalData(appellation, gender, description, bkImage, layoutImage);
        } else {
            updatePersonalData(appellation, gender, description, bkImage, layoutImage);
        }
    }

    @Transaction
    public void insertDataSafe(
            long userId,
            String appellation, short gender, String description,
            short promiseScore, short inTimeScore,
            short levelScore, short cooperationScore,
            short communicateScore,
            String bkImage, String layoutImage) {
        List<PersonalData> personalDataList = selectPersonData(userId);
        if (personalDataList.isEmpty()) {
            insertPersonalData(appellation, gender, description, promiseScore, inTimeScore, levelScore, cooperationScore, communicateScore, bkImage, layoutImage);
        } else {
            updatePersonalData(appellation, gender, description, promiseScore, inTimeScore, levelScore, cooperationScore, communicateScore, bkImage, layoutImage);
        }
    }
}
