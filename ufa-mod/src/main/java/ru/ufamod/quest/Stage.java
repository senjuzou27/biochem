package ru.ufamod.quest;

/** Where the player is in the story of the day. */
public enum Stage {
    NONE,
    WAKE,            // take the coat and the record book from the wardrobe
    BREAKFAST,       // eat something in the dormitory kitchen
    TO_BSMU,         // walk down Lenina to the university
    WATCHMAN,        // pass the guard wearing the lab coat
    LECTURE,         // find the lecture hall and talk to the lecturer
    LECTURE_RUNNING, // the biochemistry lecture is being read
    TO_EXAM,         // go to room 301 and draw an exam ticket
    EXAM_RUNNING,    // the exam screen is open
    PASSED,          // celebrate at the Salavat Yulaev monument
    FAILED,          // leave the university...
    ARREST,          // ...where the military commissariat is waiting
    CAMP_REPORT,     // report to the sergeant
    CAMP_COURSE,     // obstacle course
    CAMP_RANGE,      // shooting range
    CAMP_TRENCH,     // dig a trench
    CAMP_TRUCK,      // board the truck
    END_SVO,         // bad ending
    FREE;            // good ending / free roam

    public static Stage byName(String name) {
        for (Stage s : values()) if (s.name().equalsIgnoreCase(name)) return s;
        return NONE;
    }

    public boolean inCamp() {
        return ordinal() >= CAMP_REPORT.ordinal() && ordinal() <= END_SVO.ordinal();
    }
}
