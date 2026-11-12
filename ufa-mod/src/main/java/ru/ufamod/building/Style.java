package ru.ufamod.building;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ru.ufamod.city.BuildingInfo;

import java.util.Random;

/** What a building looks like: era, materials, storey height, window rhythm and roof. */
public final class Style {
    public enum Era { WOOD, MERCHANT, STALIN, KHRUSHCHEV, PANEL, MODERN, INDUSTRIAL, RELIGIOUS }

    public enum Use { RESIDENTIAL, HOUSE, DORM, UNIVERSITY, SCHOOL, KINDERGARTEN, OFFICE, HOSPITAL, HOTEL, RETAIL, MALL, INDUSTRIAL, GARAGE, CHURCH, MOSQUE, THEATRE, LIBRARY, MUSEUM }

    public Era era;
    public Use use;
    public int levels;
    public int groundHeight, storeyHeight;
    public Block wall, trim, plinth, roofStairs, roofBlock, roofSlab, floor, partition, door, interiorDoor, accent;
    public Block glass;
    /** Window rhythm: period and which positions in it are windows. */
    public int period;
    public boolean[] windowAt;
    public boolean tallWindows;
    public boolean pitched, flat, balconies, loggias, cornice, quoins, shutters;
    public boolean logWalls;

    private static final Block[] PLASTER = {Blocks.YELLOW_TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.PINK_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.SMOOTH_SANDSTONE, Blocks.ORANGE_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA};
    private static final Block[] OLD_ROOF = {Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DARK_PRISMARINE_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
            Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.MUD_BRICK_STAIRS};
    private static final Block[] OLD_ROOF_FULL = {Blocks.DEEPSLATE_TILES, Blocks.DARK_PRISMARINE, Blocks.WAXED_WEATHERED_CUT_COPPER,
            Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.MUD_BRICKS};
    private static final Block[] OLD_ROOF_SLAB = {Blocks.DEEPSLATE_TILE_SLAB, Blocks.DARK_PRISMARINE_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
            Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.MUD_BRICK_SLAB};

    public static Use useOf(BuildingInfo b) {
        String t = b.part.isEmpty() ? b.type : (b.type.isEmpty() ? "yes" : b.type);
        String a = b.amenity;
        if ("mosque".equals(t) || "muslim".equals(b.religion)) return Use.MOSQUE;
        if (t.equals("church") || t.equals("cathedral") || t.equals("chapel") || t.equals("temple") || "christian".equals(b.religion)) return Use.CHURCH;
        if (a.equals("theatre") || a.equals("cinema") || t.equals("theatre")) return Use.THEATRE;
        if (a.equals("library")) return Use.LIBRARY;
        if ("museum".equals(b.tourism)) return Use.MUSEUM;
        switch (t) {
            case "apartments", "residential", "terrace" -> { return Use.RESIDENTIAL; }
            case "house", "detached", "semidetached_house", "bungalow", "cabin", "farm" -> { return Use.HOUSE; }
            case "dormitory" -> { return Use.DORM; }
            case "university", "college" -> { return Use.UNIVERSITY; }
            case "school" -> { return Use.SCHOOL; }
            case "kindergarten" -> { return Use.KINDERGARTEN; }
            case "office", "government", "public", "civic", "commercial", "bank", "courthouse" -> { return Use.OFFICE; }
            case "hospital", "clinic" -> { return Use.HOSPITAL; }
            case "hotel" -> { return Use.HOTEL; }
            case "retail", "supermarket", "kiosk", "shop" -> { return b.area > 3000 ? Use.MALL : Use.RETAIL; }
            case "industrial", "warehouse", "service", "transformer_tower", "shed", "hangar", "storage_tank", "greenhouse", "boathouse" -> { return Use.INDUSTRIAL; }
            case "garage", "garages", "carport", "parking" -> { return Use.GARAGE; }
            default -> { }
        }
        if (b.shop.equals("mall") || b.area > 6000 && !b.shops.isEmpty()) return Use.MALL;
        for (BuildingInfo.Poi p : b.pois) {
            switch (p.kind()) {
                case "university", "college" -> { return Use.UNIVERSITY; }
                case "school" -> { return Use.SCHOOL; }
                case "kindergarten" -> { return Use.KINDERGARTEN; }
                case "hospital", "clinic", "doctors" -> { return Use.HOSPITAL; }
                case "hotel", "hostel" -> { return Use.HOTEL; }
                case "theatre", "cinema" -> { return Use.THEATRE; }
                case "library" -> { return Use.LIBRARY; }
                case "museum" -> { return Use.MUSEUM; }
                default -> { if (!b.office.isEmpty() || p.kind().equals("government") || p.kind().equals("townhall") || p.kind().equals("courthouse")) return Use.OFFICE; }
            }
        }
        if (!b.office.isEmpty()) return Use.OFFICE;
        if (b.area < 45) return Use.INDUSTRIAL;
        if (b.levels == 1 && b.area < 400) return b.shops.isEmpty() ? Use.HOUSE : Use.RETAIL;
        if (b.levels == 0 && b.area < 160) return Use.HOUSE;
        return Use.RESIDENTIAL;
    }

    public static int estimateLevels(BuildingInfo b, Use use) {
        if (b.levels > 0) return b.levels;
        return switch (use) {
            case HOUSE -> b.area > 140 ? 2 : 1;
            case INDUSTRIAL, GARAGE -> 1;
            case RETAIL -> b.area > 800 ? 2 : 1;
            case MALL -> 3;
            case CHURCH, MOSQUE -> 2;
            case DORM -> 5;
            case HOTEL -> 6;
            case UNIVERSITY, OFFICE, HOSPITAL -> b.area > 1500 ? 5 : 4;
            case SCHOOL -> 3;
            case KINDERGARTEN -> 2;
            case THEATRE, LIBRARY, MUSEUM -> 3;
            default -> b.area < 300 ? 2 : b.area < 700 ? 4 : 5;
        };
    }

    public static Style of(BuildingInfo b, Random r) {
        Style s = new Style();
        s.use = useOf(b);
        s.levels = Math.max(1, Math.min(30, estimateLevels(b, s.use)));
        String mat = b.material;
        int lv = s.levels;
        if (s.use == Use.CHURCH || s.use == Use.MOSQUE) s.era = Era.RELIGIOUS;
        else if (s.use == Use.INDUSTRIAL || s.use == Use.GARAGE) s.era = Era.INDUSTRIAL;
        else if (mat.equals("wood") || s.use == Use.HOUSE && lv <= 2 && !mat.equals("brick") && r.nextInt(5) != 0) s.era = Era.WOOD;
        else if (mat.equals("glass") || mat.equals("metal") || (s.use == Use.OFFICE || s.use == Use.HOTEL || s.use == Use.MALL) && lv >= 6) s.era = Era.MODERN;
        else if (mat.equals("panel") || mat.equals("concrete") && lv >= 7 || (s.use == Use.RESIDENTIAL || s.use == Use.DORM) && lv >= 9) s.era = Era.PANEL;
        else if (lv <= 3 && s.use != Use.DORM) s.era = Era.MERCHANT;
        else if ((s.use == Use.RESIDENTIAL || s.use == Use.DORM) && lv <= 5 && r.nextInt(3) != 0) s.era = Era.KHRUSHCHEV;
        else if (lv <= 8) s.era = Era.STALIN;
        else s.era = Era.PANEL;
        if (s.use == Use.THEATRE || s.use == Use.LIBRARY || s.use == Use.MUSEUM) s.era = lv >= 6 ? Era.MODERN : Era.STALIN;

        s.floor = Blocks.SPRUCE_PLANKS;
        s.partition = Blocks.WHITE_TERRACOTTA;
        s.door = Blocks.DARK_OAK_DOOR;
        s.interiorDoor = Blocks.SPRUCE_DOOR;
        s.glass = Blocks.GLASS_PANE;
        s.accent = Blocks.WHITE_CONCRETE;
        s.roofStairs = Blocks.DEEPSLATE_TILE_STAIRS;
        s.roofBlock = Blocks.DEEPSLATE_TILES;
        s.roofSlab = Blocks.DEEPSLATE_TILE_SLAB;
        s.plinth = Blocks.POLISHED_ANDESITE;
        switch (s.era) {
            case WOOD -> {
                s.wall = r.nextInt(3) == 0 ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_LOG;
                s.logWalls = true;
                s.trim = Blocks.BIRCH_PLANKS;
                s.plinth = r.nextBoolean() ? Blocks.BRICKS : Blocks.STONE_BRICKS;
                int k = r.nextInt(OLD_ROOF.length);
                s.roofStairs = OLD_ROOF[k];
                s.roofBlock = OLD_ROOF_FULL[k];
                s.roofSlab = OLD_ROOF_SLAB[k];
                s.groundHeight = s.storeyHeight = 3;
                s.period = 3;
                s.windowAt = new boolean[]{false, true, false};
                s.pitched = true;
                s.shutters = true;
                s.door = Blocks.SPRUCE_DOOR;
            }
            case MERCHANT -> {
                s.wall = r.nextInt(3) == 0 ? Blocks.BRICKS : PLASTER[r.nextInt(PLASTER.length)];
                s.trim = s.wall == Blocks.BRICKS ? Blocks.SMOOTH_SANDSTONE : Blocks.SMOOTH_QUARTZ;
                int k = r.nextInt(OLD_ROOF.length);
                s.roofStairs = OLD_ROOF[k];
                s.roofBlock = OLD_ROOF_FULL[k];
                s.roofSlab = OLD_ROOF_SLAB[k];
                s.groundHeight = s.storeyHeight = 4;
                s.period = 2;
                s.windowAt = new boolean[]{false, true};
                s.tallWindows = true;
                s.pitched = true;
                s.cornice = true;
                s.quoins = true;
            }
            case STALIN -> {
                Block[] w = {Blocks.SMOOTH_SANDSTONE, Blocks.YELLOW_TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA};
                s.wall = w[r.nextInt(w.length)];
                s.trim = Blocks.SMOOTH_QUARTZ;
                s.plinth = r.nextBoolean() ? Blocks.POLISHED_GRANITE : Blocks.STONE_BRICKS;
                s.groundHeight = s.storeyHeight = 4;
                s.period = 3;
                s.windowAt = new boolean[]{false, true, true};
                s.tallWindows = true;
                s.pitched = r.nextInt(3) != 0;
                s.cornice = true;
                s.balconies = s.use == Use.RESIDENTIAL;
                s.roofStairs = Blocks.DEEPSLATE_TILE_STAIRS;
            }
            case KHRUSHCHEV -> {
                Block[] w = {Blocks.BRICKS, Blocks.SMOOTH_SANDSTONE, Blocks.WHITE_TERRACOTTA, Blocks.BRICKS};
                s.wall = w[r.nextInt(w.length)];
                s.trim = s.wall == Blocks.BRICKS ? Blocks.SMOOTH_STONE : Blocks.LIGHT_GRAY_TERRACOTTA;
                s.groundHeight = s.storeyHeight = 3;
                s.period = 3;
                s.windowAt = new boolean[]{false, true, true};
                s.flat = true;
                s.balconies = true;
                s.door = Blocks.SPRUCE_DOOR;
            }
            case PANEL -> {
                s.wall = r.nextBoolean() ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_GRAY_CONCRETE;
                Block[] acc = {Blocks.CYAN_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.YELLOW_TERRACOTTA};
                s.accent = acc[r.nextInt(acc.length)];
                s.trim = Blocks.LIGHT_GRAY_CONCRETE;
                s.groundHeight = s.storeyHeight = 3;
                s.period = 3;
                s.windowAt = new boolean[]{false, true, true};
                s.flat = true;
                s.loggias = true;
                s.door = Blocks.IRON_DOOR;
            }
            case MODERN -> {
                Block[] w = {Blocks.LIGHT_GRAY_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.POLISHED_ANDESITE};
                s.wall = w[r.nextInt(w.length)];
                s.trim = Blocks.GRAY_CONCRETE;
                Block[] g = {Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE};
                s.glass = g[r.nextInt(g.length)];
                s.groundHeight = s.storeyHeight = 4;
                s.period = 3;
                s.windowAt = new boolean[]{false, true, true};
                s.tallWindows = true;
                s.flat = true;
                s.floor = Blocks.POLISHED_ANDESITE;
                s.door = Blocks.IRON_DOOR;
            }
            case INDUSTRIAL -> {
                Block[] w = {Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.GRAY_CONCRETE, Blocks.ANDESITE, Blocks.BRICKS, Blocks.WHITE_TERRACOTTA};
                s.wall = w[r.nextInt(w.length)];
                s.trim = Blocks.SMOOTH_STONE;
                s.groundHeight = s.storeyHeight = 4;
                s.period = 4;
                s.windowAt = new boolean[]{false, true, true, false};
                s.flat = true;
                s.floor = Blocks.SMOOTH_STONE;
                s.door = Blocks.IRON_DOOR;
            }
            case RELIGIOUS -> {
                s.wall = s.use == Use.MOSQUE ? Blocks.WHITE_CONCRETE : Blocks.SMOOTH_QUARTZ;
                s.trim = s.use == Use.MOSQUE ? Blocks.CYAN_TERRACOTTA : Blocks.YELLOW_TERRACOTTA;
                s.groundHeight = s.storeyHeight = 5;
                s.period = 3;
                s.windowAt = new boolean[]{false, true, false};
                s.tallWindows = true;
                s.pitched = true;
                s.roofStairs = s.use == Use.MOSQUE ? Blocks.DARK_PRISMARINE_STAIRS : Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS;
                s.roofBlock = s.use == Use.MOSQUE ? Blocks.DARK_PRISMARINE : Blocks.WAXED_OXIDIZED_CUT_COPPER;
                s.roofSlab = s.use == Use.MOSQUE ? Blocks.DARK_PRISMARINE_SLAB : Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB;
                s.floor = Blocks.POLISHED_DIORITE;
            }
        }
        // the tag wins over guesses
        switch (b.roof) {
            case "flat" -> { s.flat = true; s.pitched = false; }
            case "gabled", "hipped", "pyramidal", "half-hipped", "mansard", "skillion", "gambrel" -> { s.flat = false; s.pitched = true; }
            default -> { }
        }
        if (!s.pitched) s.flat = true;
        if (s.use == Use.GARAGE) s.levels = 1;
        // shops and public buildings get a taller ground floor
        if ((!b.shops.isEmpty() || s.use == Use.RETAIL || s.use == Use.MALL || s.use == Use.UNIVERSITY || s.use == Use.OFFICE) && s.groundHeight < 4) s.groundHeight = 4;
        return s;
    }

    public boolean residential() {
        return use == Use.RESIDENTIAL || use == Use.HOUSE;
    }

    public boolean corridorPlan() {
        return switch (use) {
            case DORM, UNIVERSITY, SCHOOL, KINDERGARTEN, OFFICE, HOSPITAL, HOTEL, MALL, LIBRARY, MUSEUM -> true;
            default -> false;
        };
    }
}
