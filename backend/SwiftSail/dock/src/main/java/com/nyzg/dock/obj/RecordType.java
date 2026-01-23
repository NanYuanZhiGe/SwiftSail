package com.nyzg.dock.obj;

import java.util.Collections;
import java.util.Set;

public class RecordType {
    final public static String SLEEP = "Sleep";
    final public static String STEP = "Step";
    final public static String DISTANCE = "Distance";
    final public static String CALORIC = "Caloric";
    final public static String FOOD = "Food";
    final public static String HEART = "Heart";

    final public static Set<String> TYPE_SET = Collections.unmodifiableSet(
            Set.of(
                    SLEEP,
                    STEP,
                    DISTANCE,
                    CALORIC,
                    FOOD,
                    HEART
            )
    );
}