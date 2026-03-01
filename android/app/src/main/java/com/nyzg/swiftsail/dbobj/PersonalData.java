package com.nyzg.swiftsail.dbobj;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "personalDataTable",
        indices = {
                @Index(unique = true, value = {"userId"})
        }
)
public class PersonalData {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public String appellation;
    public short gender;
    public String description;
    public short promiseScore;
    public short inTimeScore;
    public short levelScore;
    public short cooperationScore;
    public short communicateScore;
    public String bkImage;
    public String layoutImage;

    public PersonalData(){
    }

    public PersonalData(String appellation, String bkImage, short communicateScore, short cooperationScore, String description, short gender, short inTimeScore, String layoutImage, short levelScore, short promiseScore, long userId) {
        this.appellation = appellation;
        this.bkImage = bkImage;
        this.communicateScore = communicateScore;
        this.cooperationScore = cooperationScore;
        this.description = description;
        this.gender = gender;
        this.inTimeScore = inTimeScore;
        this.layoutImage = layoutImage;
        this.levelScore = levelScore;
        this.promiseScore = promiseScore;
        this.userId = userId;
    }
}