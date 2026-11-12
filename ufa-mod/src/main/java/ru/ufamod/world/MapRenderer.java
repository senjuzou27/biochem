package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import ru.ufamod.UfaMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Developer tool: renders a canvas to PNG files (plan view, floor slices and oblique views)
 * so the city can be checked without starting the game client. Enabled with -Dufamod.dump=dir.
 */
public final class MapRenderer {
    private MapRenderer() {}

    private static final int GRASS = 0x6FA84A;

    private static int color(BlockState s) {
        if (s.is(Blocks.WATER)) return 0x3F76E4;
        if (s.is(Blocks.GLASS) || s.is(Blocks.GLASS_PANE)) return 0xBFE6F2;
        MapColor mc = s.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        return mc == MapColor.NONE ? 0xD8D8D8 : mc.col;
    }

    private static int shade(int rgb, double f) {
        int r = (int) Math.max(0, Math.min(255, ((rgb >> 16) & 255) * f));
        int g = (int) Math.max(0, Math.min(255, ((rgb >> 8) & 255) * f));
        int b = (int) Math.max(0, Math.min(255, (rgb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Plan view of the highest block within [yMin, yMax]. */
    public static BufferedImage plan(Canvas c, int x1, int z1, int x2, int z2, int yMin, int yMax, int scale) {
        BufferedImage img = new BufferedImage((x2 - x1 + 1) * scale, (z2 - z1 + 1) * scale, BufferedImage.TYPE_INT_ARGB);
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                int rgb = shade(GRASS, 0.8);
                for (int y = yMax; y >= yMin; y--) {
                    BlockState s = c.get(x, y, z);
                    if (s == null || s.isAir()) continue;
                    rgb = shade(color(s), 0.62 + 0.38 * (y - yMin + 1) / (double) (yMax - yMin + 1));
                    break;
                }
                for (int i = 0; i < scale; i++)
                    for (int j = 0; j < scale; j++)
                        img.setRGB((x - x1) * scale + i, (z - z1) * scale + j, rgb);
            }
        return img;
    }

    /**
     * Oblique view. dir 0: looking north (from the south), 1: looking west (from the east),
     * 2: looking south, 3: looking east.
     */
    public static BufferedImage oblique(Canvas c, int x1, int z1, int x2, int z2, int dir, int scale) {
        boolean alongX = dir == 0 || dir == 2;
        int wide = alongX ? x2 - x1 + 1 : z2 - z1 + 1;
        int deep = alongX ? z2 - z1 + 1 : x2 - x1 + 1;
        int yLo = -6, yHi = Canvas.MIN_Y + Canvas.SY - 1;
        int h = deep + (yHi - yLo) + 2;
        int[] px = new int[wide * h];
        java.util.Arrays.fill(px, 0xFF9CC7E8);
        for (int d = 0; d < deep; d++)
            for (int a = 0; a < wide; a++) {
                int x, z;
                switch (dir) {
                    case 0 -> { x = x1 + a; z = z1 + d; }
                    case 1 -> { x = x1 + d; z = z2 - a; }
                    case 2 -> { x = x2 - a; z = z2 - d; }
                    default -> { x = x2 - d; z = z1 + a; }
                }
                for (int y = yLo; y <= yHi; y++) {
                    BlockState s = c.get(x, y, z);
                    int rgb;
                    if (s == null) {
                        if (y != 0) continue;
                        rgb = GRASS;
                    } else if (s.isAir()) continue;
                    else rgb = color(s);
                    int row = d + (yHi - y);
                    px[row * wide + a] = shade(rgb, 1.0);
                    if (row + 1 < h) px[(row + 1) * wide + a] = shade(rgb, 0.72);
                }
            }
        BufferedImage img = new BufferedImage(wide * scale, h * scale, BufferedImage.TYPE_INT_ARGB);
        for (int r = 0; r < h; r++)
            for (int a = 0; a < wide; a++)
                for (int i = 0; i < scale; i++)
                    for (int j = 0; j < scale; j++)
                        img.setRGB(a * scale + i, r * scale + j, px[r * wide + a]);
        return img;
    }

    private static void save(BufferedImage img, File dir, String name) throws IOException {
        ImageIO.write(img, "png", new File(dir, name + ".png"));
    }

    public static void dumpAll(Canvas city, Canvas camp, File dir) throws IOException {
        dir.mkdirs();
        int top = Canvas.MIN_Y + Canvas.SY - 1;
        save(plan(city, 0, 0, city.sx - 1, city.sz - 1, -8, top, 2), dir, "city_plan");
        save(oblique(city, 0, 0, city.sx - 1, city.sz - 1, 0, 2), dir, "city_from_south");
        // BSMU: every storey as a floor plan, then the front seen from Lenina and the whole quarter
        for (int s = 1; s <= 4; s++)
            save(plan(city, 280, 334, 322, 438, Plan.bsFloor(s), Plan.bsFloor(s) + 2, 8), dir, "bsmu_floor" + s);
        save(oblique(city, 280, 330, 336, 440, 1, 6), dir, "bsmu_from_east");
        save(oblique(city, 216, 322, 336, 442, 0, 4), dir, "bsmu_quarter_from_south");
        save(plan(city, 336, 10, 364, 66, 0, 2, 10), dir, "dorm_floor");
        save(oblique(city, 330, 8, 366, 68, 3, 6), dir, "dorm_from_west");
        save(oblique(city, 176, 86, 320, 190, 0, 4), dir, "gostiny_from_south");
        save(oblique(city, 236, 206, 336, 310, 1, 5), dir, "opera_from_east");
        save(oblique(city, 236, 206, 322, 312, 0, 5), dir, "opera_from_south");
        save(oblique(city, 26, 574, 114, 690, 0, 6), dir, "salavat_from_south");
        save(oblique(city, 26, 574, 114, 690, 1, 6), dir, "salavat_from_east");
        save(oblique(city, 560, 574, 636, 700, 0, 5), dir, "friendship_from_south");
        save(oblique(city, 296, 574, 536, 664, 2, 4), dir, "tukaeva_from_north");
        save(oblique(city, 254, 454, 450, 560, 0, 4), dir, "park_from_south");
        save(oblique(city, 44, 454, 254, 560, 2, 4), dir, "west_from_north");
        save(oblique(city, 336, 86, 450, 190, 0, 4), dir, "artkvadrat_from_south");
        save(oblique(city, 336, 324, 560, 440, 0, 4), dir, "library_sovetskaya_from_south");
        save(oblique(city, 300, 0, 360, 460, 1, 3), dir, "lenina_west_side");
        save(plan(camp, 0, 0, camp.sx - 1, camp.sz - 1, -4, top, 5), dir, "camp_plan");
        save(oblique(camp, 0, 0, camp.sx - 1, camp.sz - 1, 0, 5), dir, "camp_from_south");
        UfaMod.LOGGER.info("Map images written to {}", dir.getAbsolutePath());
    }
}
