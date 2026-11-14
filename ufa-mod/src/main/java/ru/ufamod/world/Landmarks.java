package ru.ufamod.world;

import net.minecraft.world.level.block.Blocks;
import ru.ufamod.building.BuildingGenerator.Hints;
import ru.ufamod.building.Style.Era;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * How notable buildings of the centre really look, from street panoramas: wall and trim materials, glass,
 * roofs, porticos, domes. Keyed by OpenStreetMap id (negative for relations), so it survives a rebuild of the map.
 */
public final class Landmarks {
    private Landmarks() {}

    private static final Map<Long, Consumer<Hints>> LOOKS = new HashMap<>();

    private static void look(long osm, Consumer<Hints> h) {
        LOOKS.put(osm, h);
    }

    static {
        // Congress hall "Toratau", Zaki Validi 2: a long base of pale stone under a band of turquoise glass
        look(-17051651L, h -> {
            h.era = Era.MODERN;
            h.wall = Blocks.SMOOTH_SANDSTONE;
            h.trim = Blocks.CALCITE;
            h.glass = Blocks.CYAN_STAINED_GLASS_PANE.defaultBlockState();
            h.noPortico = true;
        });
        // Ar-Rahim cathedral mosque: white walls, golden domes, four tall minarets
        look(986804518L, h -> {
            h.wall = Blocks.SMOOTH_QUARTZ;
            h.trim = Blocks.QUARTZ_PILLAR;
            h.domeBlock = Blocks.GOLD_BLOCK;
            h.minarets = 4;
        });
        // House of the Government, Tukaeva 46: white with tall white pilasters
        look(-1501154L, h -> {
            h.era = Era.STALIN;
            h.wall = Blocks.WHITE_CONCRETE;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.base = Blocks.POLISHED_DIORITE;
            h.pilasters = true;
            h.pitched = Boolean.FALSE;
            h.glass = Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.defaultBlockState();
        });
        // Palace of the Trade Unions, Kirova 1: dark red brick over a base of light stone
        look(79540944L, h -> {
            h.era = Era.STALIN;
            h.wall = Blocks.BRICKS;
            h.trim = Blocks.SMOOTH_SANDSTONE;
            h.base = Blocks.SMOOTH_SANDSTONE;
            h.pitched = Boolean.FALSE;
            h.noPortico = true;
        });
        // Crowne Plaza (ex Sheraton), Tsyurupy 7: grey panels and blue glass
        look(435417940L, h -> {
            h.era = Era.MODERN;
            h.wall = Blocks.LIGHT_GRAY_CONCRETE;
            h.trim = Blocks.GRAY_CONCRETE;
            h.glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState();
        });
        // the old Zemstvo board, Tsyurupy 12: coral red with white decoration
        look(95783205L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.PINK_TERRACOTTA;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.pitched = Boolean.TRUE;
        });
        // State concert hall "Bashkortostan", Lenina 50: white fins, a broad bronze crown, dark granite below
        look(88880374L, h -> {
            h.era = Era.MODERN;
            h.wall = Blocks.SMOOTH_QUARTZ;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.base = Blocks.BROWN_TERRACOTTA;
            h.pilasters = true;
            h.crown = Blocks.WAXED_CUT_COPPER;
            h.glass = Blocks.GRAY_STAINED_GLASS_PANE.defaultBlockState();
        });
        // Bashkir drama theatre, Zaki Validi 34: white walls under green roofs
        look(95809680L, h -> {
            h.era = Era.STALIN;
            h.wall = Blocks.SMOOTH_QUARTZ;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.pitched = Boolean.TRUE;
            h.roofBlock = Blocks.WAXED_OXIDIZED_CUT_COPPER;
            h.roofStairs = Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS;
            h.roofSlab = Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB;
            h.roomType = "hall";
        });
        // Philharmonic, Gogolya 58: mint walls, white decoration, a dark plinth
        look(95809575L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.WAXED_OXIDIZED_COPPER;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.base = Blocks.POLISHED_DEEPSLATE;
            h.pitched = Boolean.FALSE;
            h.roomType = "hall";
        });
        // First Ufa cathedral mosque, Tukaeva 52: white with a green dome and a white minaret
        look(95809606L, h -> {
            h.wall = Blocks.SMOOTH_QUARTZ;
            h.domeBlock = Blocks.WAXED_OXIDIZED_COPPER;
            h.minarets = 1;
        });
        // Hotel "Bashkiria", Lenina 25/29: cream walls over grey granite
        look(89400260L, h -> {
            h.era = Era.STALIN;
            h.wall = Blocks.SMOOTH_SANDSTONE;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.base = Blocks.POLISHED_ANDESITE;
            h.pitched = Boolean.FALSE;
            h.noPortico = true;
        });
        // Lenina 7: rich red brick with a round corner tower
        look(-1398838L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.RED_TERRACOTTA;
            h.trim = Blocks.RED_TERRACOTTA;
            h.base = Blocks.POLISHED_ANDESITE;
            h.pitched = Boolean.FALSE;
        });
        // the main building of the university, Zaki Validi 32: white with a columned portico
        look(95809675L, h -> {
            h.era = Era.STALIN;
            h.wall = Blocks.SMOOTH_QUARTZ;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.portico = true;
            h.pitched = Boolean.FALSE;
        });
        // Alexander passage, Karla Marksa 20: cream walls, dark window frames
        look(95200807L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.SMOOTH_SANDSTONE;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.pitched = Boolean.TRUE;
            h.roofBlock = Blocks.DEEPSLATE_TILES;
            h.roofStairs = Blocks.DEEPSLATE_TILE_STAIRS;
            h.roofSlab = Blocks.DEEPSLATE_TILE_SLAB;
        });
        // Lenina 6: a pale yellow two-storey house with white decoration
        look(98970890L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.SMOOTH_SANDSTONE;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.pitched = Boolean.FALSE;
        });
        // the governor's house, Tukaeva 23: yellow with white columns on a red granite base
        look(95783297L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.SMOOTH_SANDSTONE;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.base = Blocks.POLISHED_GRANITE;
            h.portico = true;
            h.pitched = Boolean.FALSE;
        });
        // House of the Officers, Kommunisticheskaya 43: red brick with white trim
        look(95236248L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.BRICKS;
            h.trim = Blocks.SMOOTH_QUARTZ;
            h.pitched = Boolean.TRUE;
        });
        // hotel "Ufa-Astoria", Karla Marksa 25: white and richly decorated, brick details
        look(95394183L, h -> {
            h.era = Era.MERCHANT;
            h.wall = Blocks.SMOOTH_QUARTZ;
            h.trim = Blocks.BRICKS;
            h.pitched = Boolean.FALSE;
        });
    }

    /** Puts the known look of a building into the hints; returns false for buildings without one. */
    public static boolean apply(long osm, Hints hints) {
        Consumer<Hints> h = LOOKS.get(osm);
        if (h == null) return false;
        h.accept(hints);
        return true;
    }

    public static int count() {
        return LOOKS.size();
    }
}
