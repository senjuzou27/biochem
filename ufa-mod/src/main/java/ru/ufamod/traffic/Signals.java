package ru.ufamod.traffic;

/**
 * All traffic lights of the city run on one clock: first the traffic along the X axis has green,
 * then the traffic along the Z axis.
 */
public final class Signals {
    private Signals() {}

    public static final int CYCLE = 600, GREEN = 240, YELLOW = 60;

    /** 0: X green, Z red; 1: X yellow; 2: X red, Z green; 3: Z yellow. */
    public static int state(long gameTime) {
        int t = (int) Math.floorMod(gameTime, (long) CYCLE);
        if (t < GREEN) return 0;
        if (t < GREEN + YELLOW) return 1;
        if (t < 2 * GREEN + YELLOW) return 2;
        return 3;
    }

    public static boolean green(boolean alongX, long gameTime) {
        return state(gameTime) == (alongX ? 0 : 2);
    }
}
