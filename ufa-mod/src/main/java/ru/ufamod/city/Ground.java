package ru.ufamod.city;

/** Codes of the per-cell layers written by tools/osm_to_city.py. */
public final class Ground {
    private Ground() {}

    public static final int VOID = 0, GRASS = 1, ROAD = 2, SIDEWALK = 3, PLAZA = 4, PATH = 5, PARKING = 6, WATER = 7, SAND = 8,
            PITCH = 9, PLAYGROUND = 10, FLOWERBED = 11, FOREST = 12, DIRT = 13, YARD = 14, STEPS = 15, TRACK = 16, GARAGES = 17,
            FOUNTAIN = 18, BRIDGE = 19;

    // markings
    public static final int M_NONE = 0, M_LINE = 1, M_ZEBRA = 2, M_RAIL = 3, M_CURB = 4, M_PARKING = 5, M_STOP = 6;

    // line features
    public static final int F_NONE = 0, F_FENCE = 1, F_WALL = 2, F_HEDGE = 3, F_RETAINING = 4, F_WOODFENCE = 5, F_GUARD = 6;

    public static boolean paved(int g) {
        return g == ROAD || g == SIDEWALK || g == PLAZA || g == PARKING || g == YARD || g == GARAGES || g == STEPS;
    }

    public static boolean green(int g) {
        return g == GRASS || g == FOREST || g == FLOWERBED || g == VOID;
    }
}
