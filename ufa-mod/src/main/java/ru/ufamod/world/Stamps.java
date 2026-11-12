package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.building.Model;
import ru.ufamod.city.CityData;
import ru.ufamod.city.MapPoint;
import ru.ufamod.quest.Loot;

import java.util.ArrayList;
import java.util.List;

/** Hand-made objects placed over the generated city: the two great monuments and the training camp. */
public final class Stamps {
    private Stamps() {}

    private static volatile List<Model> all;

    public static List<Model> all() {
        List<Model> a = all;
        if (a == null) {
            synchronized (Stamps.class) {
                a = all;
                if (a == null) {
                    a = new ArrayList<>();
                    CityData city = CityData.get();
                    a.add(salavat(city));
                    a.add(friendship(city));
                    a.add(fromCanvas(CampBuilder.buildCamp()));
                    all = a;
                }
            }
        }
        return a;
    }

    /** Map points that a stamp draws itself. */
    public static boolean replacesPoint(MapPoint p) {
        String n = p.name().toLowerCase();
        return n.contains("салават") && p.kind() != MapPoint.Kind.KIOSK && p.kind() != MapPoint.Kind.BUS_STOP || n.contains("монумент дружбы");
    }

    static Model fromCanvas(Canvas c) {
        Model m = new Model(c.ox, c.baseY + Canvas.MIN_Y, c.oz, c.sx, Canvas.SY, c.sz);
        for (int x = 0; x < c.sx; x++)
            for (int z = 0; z < c.sz; z++)
                for (int y = Canvas.MIN_Y; y < Canvas.MIN_Y + Canvas.SY; y++) {
                    BlockState s = c.get(x, y, z);
                    if (s != null) m.set(c.ox + x, c.baseY + y, c.oz + z, s);
                }
        for (Canvas.SignSpec s : c.signs) m.signText(c.ox + s.x(), c.baseY + s.y(), c.oz + s.z(), s.lines());
        for (Canvas.ChestSpec s : c.chests) m.chestItems(c.ox + s.x(), c.baseY + s.y(), c.oz + s.z(), Loot.of(s.loot()));
        for (Canvas.NpcSpec s : c.npcs) m.npc(c.ox + s.x(), c.baseY + s.y(), c.oz + s.z(), s.yaw(), s.role(), s.name(), s.variant(), s.wander());
        return m;
    }

    private static void fill(Model m, int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) m.set(x, y, z, b.defaultBlockState());
    }

    private static void sign(Model m, int x, int y, int z, Direction facing, String... lines) {
        int rot = switch (facing) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            default -> 12;
        };
        m.set(x, y, z, Blocks.BIRCH_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot));
        m.signText(x, y, z, lines);
    }

    /** A level paved disc: fills below, clears above. */
    private static void terrace(Model m, int cx, int y, int cz, int r, Block inner, int innerR) {
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                int d = dx * dx + dz * dz;
                if (d > r * r) continue;
                fill(m, cx + dx, y - 4, cz + dz, cx + dx, y - 1, cz + dz, Blocks.STONE);
                Block b = d <= innerR * innerR ? inner : ((dx + dz) & 1) == 0 ? Blocks.STONE_BRICKS : Blocks.POLISHED_ANDESITE;
                m.set(cx + dx, y, cz + dz, b.defaultBlockState());
                fill(m, cx + dx, y + 1, cz + dz, cx + dx, y + 8, cz + dz, Blocks.AIR);
            }
    }

    // ------------------------------------------------------------------ Salavat Yulaev

    private static Model salavat(CityData city) {
        int[] s = city.spots.get("salavat");
        int cx = s[0], cz = s[1], y = city.height(cx, cz);
        Model m = new Model(cx - 18, y - 4, cz - 18, 37, 34, 37);
        terrace(m, cx, y, cz, 16, Blocks.POLISHED_GRANITE, 9);
        // granite pedestal; the 9.8 m horseman looks at the Belaya (south in the game)
        fill(m, cx - 3, y + 1, cz - 7, cx + 3, y + 1, cz + 7, Blocks.POLISHED_GRANITE);
        fill(m, cx - 2, y + 2, cz - 6, cx + 2, y + 5, cz + 6, Blocks.POLISHED_GRANITE);
        horseman(m, cx, y + 6, cz - 5, Blocks.POLISHED_BLACKSTONE);
        sign(m, cx + 5, y + 1, cz - 9, Direction.NORTH, "Памятник", "Салавату Юлаеву", "1967 г.", "ск. С. Тавасиев");
        sign(m, cx - 5, y + 1, cz - 9, Direction.NORTH, "Высота 9,8 м,", "масса 40 тонн,", "всего три", "точки опоры");
        for (int a = 0; a < 8; a++) {
            double t = Math.PI * 2 * a / 8 + 0.39;
            int lx = cx + (int) Math.round(Math.cos(t) * 13), lz = cz + (int) Math.round(Math.sin(t) * 13);
            fill(m, lx, y + 1, lz, lx, y + 3, lz, Blocks.POLISHED_DEEPSLATE_WALL);
            m.set(lx, y + 4, lz, Blocks.LANTERN.defaultBlockState());
        }
        return m;
    }

    /** An equestrian statue about 13 blocks tall standing on (x, y, z..z+11); the rearing horse looks towards +z. */
    private static void horseman(Model m, int x, int y, int z, Block b) {
        for (int sx : new int[]{-1, 1}) {
            fill(m, x + sx, y, z + 1, x + sx, y + 3, z + 1, b);
            m.set(x + sx, y, z + 2, b.defaultBlockState());
        }
        // front legs lifted
        fill(m, x - 1, y + 2, z + 8, x - 1, y + 4, z + 8, b);
        m.set(x - 1, y + 2, z + 9, b.defaultBlockState());
        fill(m, x + 1, y + 4, z + 8, x + 1, y + 4, z + 10, b);
        m.set(x + 1, y + 3, z + 10, b.defaultBlockState());
        // body, tilted up at the front
        fill(m, x - 1, y + 4, z + 1, x + 1, y + 6, z + 4, b);
        fill(m, x - 1, y + 5, z + 5, x + 1, y + 7, z + 8, b);
        // tail
        fill(m, x, y + 3, z, x, y + 6, z, b);
        fill(m, x, y + 2, z - 1, x, y + 4, z - 1, b);
        // neck and head
        fill(m, x, y + 8, z + 8, x, y + 10, z + 8, b);
        fill(m, x, y + 9, z + 9, x, y + 11, z + 9, b);
        fill(m, x, y + 10, z + 10, x, y + 11, z + 11, b);
        m.set(x, y + 12, z + 9, b.defaultBlockState());
        // rider: legs, torso, shoulders, head, fur hat
        for (int sx : new int[]{-2, 2}) fill(m, x + sx, y + 4, z + 4, x + sx, y + 6, z + 5, b);
        fill(m, x, y + 7, z + 4, x, y + 10, z + 5, b);
        fill(m, x - 1, y + 9, z + 4, x + 1, y + 10, z + 5, b);
        m.set(x, y + 11, z + 5, b.defaultBlockState());
        fill(m, x - 1, y + 12, z + 4, x + 1, y + 12, z + 5, b);
        // the right hand with the whip thrown up
        m.set(x + 2, y + 10, z + 4, b.defaultBlockState());
        m.set(x + 2, y + 11, z + 3, b.defaultBlockState());
        m.set(x + 2, y + 12, z + 3, b.defaultBlockState());
        fill(m, x + 2, y + 13, z + 2, x + 2, y + 13, z + 3, Blocks.IRON_BARS);
    }

    // ------------------------------------------------------------------ Friendship monument

    private static Model friendship(CityData city) {
        int[] s = city.spots.get("friendship");
        int cx = s[0], cz = s[1], y = city.height(cx, cz);
        Model m = new Model(cx - 16, y - 4, cz - 16, 33, 44, 33);
        terrace(m, cx, y, cz, 14, Blocks.POLISHED_ANDESITE, 8);
        // round base of blue granite with bronze reliefs
        for (int dx = -6; dx <= 6; dx++)
            for (int dz = -6; dz <= 6; dz++)
                if (dx * dx + dz * dz <= 38) {
                    fill(m, cx + dx, y + 1, cz + dz, cx + dx, y + 3, cz + dz, Blocks.POLISHED_ANDESITE);
                    if (dx * dx + dz * dz > 26) m.set(cx + dx, y + 2, cz + dz, Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState());
                }
        // two 30-metre steles of pink granite: a sword driven into the ground
        for (int sx : new int[]{-1, 1}) {
            fill(m, cx + sx, y + 4, cz - 2, cx + sx, y + 22, cz + 2, Blocks.POLISHED_GRANITE);
            fill(m, cx + sx, y + 23, cz - 1, cx + sx, y + 30, cz + 1, Blocks.POLISHED_GRANITE);
            fill(m, cx + sx, y + 31, cz, cx + sx, y + 34, cz, Blocks.POLISHED_GRANITE);
        }
        // the two seated bronze women: Russia and Bashkiria
        for (int sx : new int[]{-5, 5}) {
            fill(m, cx + sx, y + 4, cz - 1, cx + sx, y + 4, cz + 1, Blocks.POLISHED_GRANITE);
            fill(m, cx + sx, y + 5, cz, cx + sx, y + 7, cz, Blocks.WAXED_EXPOSED_COPPER);
            m.set(cx + sx, y + 5, cz + 1, Blocks.WAXED_EXPOSED_COPPER.defaultBlockState());
            m.set(cx + sx + (sx < 0 ? 1 : -1), y + 6, cz, Blocks.WAXED_EXPOSED_COPPER.defaultBlockState());
        }
        sign(m, cx, y + 1, cz - 9, Direction.NORTH, "Монумент Дружбы", "1557 — 1957", "400 лет вместе", "открыт в 1965 г.");
        sign(m, cx + 4, y + 1, cz - 9, Direction.NORTH, "Слава великой", "братской дружбе", "русского и башк.", "народов");
        for (int sx : new int[]{-11, 11}) {
            fill(m, cx + sx, y + 1, cz - 5, cx + sx, y + 3, cz - 5, Blocks.POLISHED_DEEPSLATE_WALL);
            m.set(cx + sx, y + 4, cz - 5, Blocks.LANTERN.defaultBlockState());
        }
        return m;
    }
}
