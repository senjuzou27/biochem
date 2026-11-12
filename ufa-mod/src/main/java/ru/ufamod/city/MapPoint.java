package ru.ufamod.city;

import javax.annotation.Nullable;

/** A small object from the map: tree, lamp, bench, bus stop, monument ... */
public record MapPoint(Kind kind, int x, int z, String name, @Nullable String name2, @Nullable String shop, float dx, float dz) {
    public enum Kind {
        TREE, BENCH, LAMP, BUS_STOP, SIGNALS, FOUNTAIN, STATUE, BUST, STELE, MEMORIAL, KIOSK, STREET_SIGN, WASTE, BICYCLE,
        ARTWORK, CONIFER, BIRCH, FLOWERS;

        public static Kind byId(int id) {
            Kind[] v = values();
            return id >= 0 && id < v.length ? v[id] : TREE;
        }

        /** How far the object can reach from its anchor column. */
        public int radius() {
            return switch (this) {
                case TREE, CONIFER, BIRCH -> 4;
                case BUS_STOP, FOUNTAIN, STATUE, MEMORIAL, KIOSK, ARTWORK -> 5;
                default -> 2;
            };
        }
    }
}
