package com.nyzg.geo.dbobj;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PersonalData {
    long userId;
    String appellation;
    short gender;
    String description;
    short promiseScore;
    short inTimeScore;
    short levelScore;
    short cooperationScore;
    short communicateScore;
    String bkImage;
    String layoutImage;
}
