package ru.ufamod.entity;

/** Who an NPC is. The role decides the skin and what happens when the player talks to it. */
public enum NpcRole {
    CITIZEN("citizen", 4),
    STUDENT("student", 2),
    ROOMMATE("student", 2),
    WATCHMAN("watchman", 2),
    COOK("cook", 1),
    LECTURER("lecturer", 1),
    LIBRARIAN("librarian", 1),
    PROFESSOR("professor", 1),
    COMMISSAR("commissar", 1),
    SOLDIER("soldier", 1),
    SERGEANT("sergeant", 1),
    RECRUIT("recruit", 1);

    public final String skin;
    public final int variants;

    NpcRole(String skin, int variants) {
        this.skin = skin;
        this.variants = variants;
    }

    public static NpcRole byId(int id) {
        NpcRole[] v = values();
        return id >= 0 && id < v.length ? v[id] : CITIZEN;
    }
}
