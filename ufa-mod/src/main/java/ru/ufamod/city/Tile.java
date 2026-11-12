package ru.ufamod.city;

/** 256x256 cells of the city layers. */
public final class Tile {
    public static final int SIZE = 256;
    public final short[] height;
    public final byte[] ground, mark, feat;
    public final short[] building;

    public Tile(short[] height, byte[] ground, byte[] mark, byte[] feat, short[] building) {
        this.height = height;
        this.ground = ground;
        this.mark = mark;
        this.feat = feat;
        this.building = building;
    }

    public static Tile flat(int y) {
        int n = SIZE * SIZE;
        short[] h = new short[n];
        java.util.Arrays.fill(h, (short) y);
        byte[] g = new byte[n];
        java.util.Arrays.fill(g, (byte) Ground.GRASS);
        return new Tile(h, g, new byte[n], new byte[n], new short[n]);
    }
}
