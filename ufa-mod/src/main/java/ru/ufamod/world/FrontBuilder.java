package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.entity.NpcRole;

import static ru.ufamod.world.Plan.*;

/**
 * The front line of the bad ending. An invented place without a name: a trench with a firing step behind
 * a parapet of sandbags, a dugout, a field with craters and wire, a tree line on the far side and a
 * ruined hamlet in the rear. Ground level is canvas y = 0; one stands on y = 1.
 */
public class FrontBuilder extends Build {
    private static final int TRENCH_X = 58, Z1 = 24, Z2 = 136;
    private final BlockState sandbag, wire;

    public FrontBuilder(Canvas c) {
        super(c, 23L);
        BlockState sb = Military.block("superbwarfare:sandbag"), bw = Military.block("superbwarfare:barbed_wire");
        sandbag = sb != null ? sb : st(Blocks.PACKED_MUD);
        wire = bw != null ? bw : st(Blocks.COBWEB);
    }

    public static Canvas buildFront() {
        Canvas c = new Canvas(FRONT_OX, BASE_Y, FRONT_OZ, FRONT_W, FRONT_D);
        new FrontBuilder(c).build();
        return c;
    }

    private void build() {
        ground();
        field();
        trench();
        dugout();
        observationPost();
        treeLine();
        hamlet();
        truck();
    }

    // ------------------------------------------------------------------ ground

    private void ground() {
        Block[] mix = {Blocks.GRASS_BLOCK, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.DIRT, Blocks.MUD, Blocks.ROOTED_DIRT};
        for (int x = 0; x < FRONT_W; x++)
            for (int z = 0; z < FRONT_D; z++) {
                set(x, 0, z, mix[rnd.nextInt(mix.length)]);
                for (int y = -6; y < 0; y++) set(x, y, z, Blocks.DIRT);
                if (rnd.nextInt(9) == 0) set(x, 1, z, Blocks.GRASS);
                else if (rnd.nextInt(40) == 0) set(x, 1, z, Blocks.DEAD_BUSH);
            }
    }

    private void crater(int cx, int cz, int r) {
        for (int dx = -r - 1; dx <= r + 1; dx++)
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = cx + dx, z = cz + dz;
                if (!c.inside(x, 0, z)) continue;
                if (d <= r) {
                    int depth = (int) Math.round((r - d) * 0.8) + 1;
                    for (int y = 1 - depth; y <= 1; y++) set(x, y, z, AIR);
                    set(x, -depth, z, rnd.nextBoolean() ? Blocks.MUD : Blocks.COARSE_DIRT);
                } else if (d <= r + 1.2) {
                    set(x, 0, z, Blocks.COARSE_DIRT);
                    if (rnd.nextInt(3) == 0) set(x, 1, z, Blocks.COARSE_DIRT);
                    else set(x, 1, z, AIR);
                }
            }
    }

    private void field() {
        // the craters the enemy takes cover in, and many more
        for (int[] p : FRONT_COVER) crater(p[0], p[1], 2);
        for (int[] p : FRONT_COVER_MID) crater(p[0], p[1], 2);
        for (int[] p : FRONT_COVER_NEAR) crater(p[0], p[1], 1);
        for (int i = 0; i < 70; i++) {
            int x = 82 + rnd.nextInt(68), z = 6 + rnd.nextInt(FRONT_D - 12);
            crater(x, z, 1 + rnd.nextInt(3));
        }
        for (int i = 0; i < 12; i++) crater(4 + rnd.nextInt(50), 6 + rnd.nextInt(FRONT_D - 12), 1 + rnd.nextInt(2));
        // two rows of wire with a lane to the observation post
        for (int z = Z1 - 6; z <= Z2 + 6; z++) {
            if (z >= 38 && z <= 42) continue;
            set(68, 1, z, wire);
            if (z % 2 == 0) set(71, 1, z, wire);
            if (z % 6 == 0) {
                set(68, 1, z, Blocks.SPRUCE_FENCE);
                set(68, 2, z, Blocks.SPRUCE_FENCE);
            }
        }
        // a burnt-out armoured vehicle in the field
        int bx = 126, bz = 70;
        fill(bx, 1, bz, bx + 6, 1, bz + 2, Blocks.BLACKSTONE);
        fill(bx + 1, 2, bz, bx + 5, 2, bz + 2, Blocks.COAL_BLOCK);
        fill(bx + 2, 3, bz + 1, bx + 3, 3, bz + 1, Blocks.POLISHED_BLACKSTONE);
        fill(bx - 2, 3, bz + 1, bx + 1, 3, bz + 1, Blocks.IRON_BARS);
        set(bx + 4, 3, bz, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
        // fires left by the shelling: they light up whoever passes them
        int[][] fires = {{88, 36}, {94, 70}, {90, 104}, {108, 52}, {106, 86}, {104, 122}, {84, 128}};
        for (int[] f : fires) {
            set(f[0], 1, f[1], Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true)
                    .setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, false));
            set(f[0] + 1, 1, f[1], Blocks.COARSE_DIRT);
        }
        // fallen poles of a power line along the old road
        for (int z = 10; z < FRONT_D - 10; z += 22) {
            fill(34, 1, z, 34, rnd.nextBoolean() ? 6 : 2, z, Blocks.STRIPPED_SPRUCE_LOG);
        }
    }

    // ------------------------------------------------------------------ the trench

    private void trench() {
        for (int z = Z1; z <= Z2; z++) {
            // a zigzag every 16 blocks so that no one can shoot along the whole length
            int jog = ((z - Z1) / 16) % 2 == 0 ? 0 : 1;
            int x0 = TRENCH_X + jog;
            for (int x = x0; x <= x0 + 2; x++) {
                set(x, -2, z, Blocks.SPRUCE_PLANKS);          // duckboards
                set(x, -1, z, AIR);
                set(x, 0, z, AIR);
                set(x, 1, z, AIR);
            }
            // the firing step on the enemy side, revetted walls
            set(x0 + 2, -1, z, Blocks.PACKED_MUD);
            set(x0 - 1, -1, z, Blocks.SPRUCE_PLANKS);
            set(x0 - 1, 0, z, Blocks.STRIPPED_SPRUCE_LOG);
            set(x0 + 3, -1, z, Blocks.STRIPPED_SPRUCE_LOG);
            set(x0 + 3, 0, z, Blocks.STRIPPED_SPRUCE_LOG);
            // the parapet: a low row of sandbags to fire over standing on the step and to hide behind
            // crouching, and taller stacks every few blocks for cover when moving along the trench
            set(x0 + 3, 1, z, st(Blocks.MUD_BRICK_SLAB));
            if (z % 4 == 0) {
                set(x0 + 4, 1, z, sandbag);
                set(x0 + 4, 2, z, sandbag);
            }
            // the spoil heap behind
            set(x0 - 2, 1, z, Blocks.COARSE_DIRT);
            if (z % 9 == 4) set(x0 + 1, -1, z, Blocks.LANTERN);
            else if (z % 11 == 6) set(x0, -1, z, Blocks.BARREL);
        }
        // the ends of the trench
        fill(TRENCH_X - 1, -1, Z1 - 1, TRENCH_X + 3, 1, Z1 - 1, Blocks.STRIPPED_SPRUCE_LOG);
        fill(TRENCH_X - 1, -1, Z2 + 1, TRENCH_X + 4, 1, Z2 + 1, Blocks.STRIPPED_SPRUCE_LOG);
        post(TRENCH_X - 3, 1, 70, rot(Direction.EAST), "", "Позиция", "", "");
    }

    // ------------------------------------------------------------------ the dugout

    private void dugout() {
        int x1 = 36, x2 = 52, z1 = 72, z2 = 88;
        // dug in: floor at -3, three blocks of room, a log roof and earth on top
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean wall = x == x1 || x == x2 || z == z1 || z == z2;
                set(x, -3, z, wall ? st(Blocks.DIRT) : st(Blocks.SPRUCE_PLANKS));
                for (int y = -2; y <= 0; y++) set(x, y, z, wall ? Blocks.STRIPPED_OAK_LOG : Blocks.AIR);
                set(x, 1, z, Blocks.OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.Z));
                set(x, 2, z, Blocks.COARSE_DIRT);
                if (x > x1 && x < x2 && z > z1 && z < z2 && (x + z) % 3 == 0) set(x, 3, z, Blocks.DIRT);
            }
        // posts under the roof beams
        for (int z = z1 + 4; z < z2; z += 4) {
            fill(x1 + 5, -2, z, x1 + 5, 0, z, Blocks.SPRUCE_LOG);
            fill(x1 + 11, -2, z, x1 + 11, 0, z, Blocks.SPRUCE_LOG);
        }
        // the way in from the trench: a short passage and two steps down
        for (int x = x2; x <= TRENCH_X; x++)
            for (int z = 79; z <= 81; z++) {
                set(x, x <= x2 + 1 ? -3 : -2, z, Blocks.SPRUCE_PLANKS);
                for (int y = x <= x2 + 1 ? -2 : -1; y <= 0; y++) set(x, y, z, AIR);
                if (x > x2) set(x, 1, z, z == 80 ? Blocks.OAK_LOG.defaultBlockState() : st(Blocks.COARSE_DIRT));
            }
        for (int z = 79; z <= 81; z++) set(x2 + 2, -2, z, stair(Blocks.SPRUCE_STAIRS, Direction.EAST));
        set(x2 + 1, -2, 78, AIR);
        // inside: bunks, a table with a field radio, ammunition crates, lamps
        for (int x = x1 + 2; x <= x2 - 6; x += 3) {
            bed(x, -2, z2 - 1, Direction.SOUTH, Blocks.GREEN_BED);
            bed(x, -2, z1 + 1, Direction.NORTH, Blocks.GREEN_BED);
        }
        fill(x1 + 2, -2, 80, x1 + 3, -2, 80, Blocks.SPRUCE_FENCE);
        set(x1 + 2, -1, 80, Blocks.LECTERN);
        set(x1 + 3, -1, 80, Blocks.WHITE_CARPET);
        for (int z = z1 + 3; z <= z2 - 3; z += 4) {
            set(x2 - 2, -2, z, Blocks.BARREL);
            set(x2 - 2, -1, z, rnd.nextBoolean() ? Blocks.BARREL : Blocks.AIR);
        }
        for (int[] l : new int[][]{{x1 + 3, 76}, {x1 + 3, 84}, {x1 + 9, 80}, {x2 - 3, 80}})
            set(l[0], 0, l[1], Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        // a medical corner with a white cot
        bed(x1 + 2, -2, 85, Direction.WEST, Blocks.WHITE_BED);
        set(x1 + 1, -2, 87, Blocks.BREWING_STAND);
        npc(FRONT_COMMANDER.getX() - FRONT_OX + 0.5, -2, FRONT_COMMANDER.getZ() - FRONT_OZ + 0.5, yawOf(Direction.EAST), NpcRole.COMMANDER, "Командир взвода «Сокол»", 0, false);
        npc(FRONT_MEDIC.getX() - FRONT_OX + 0.5, -2, FRONT_MEDIC.getZ() - FRONT_OZ + 0.5, yawOf(Direction.EAST), NpcRole.MEDIC, "Санинструктор Лена", 0, false);
        npc(x1 + 8.5, -2, z1 + 3.5, yawOf(Direction.SOUTH), NpcRole.RECRUIT, "Мобилизованный Саня", 0, false);
        // two more soldiers on watch in the trench
        npc(TRENCH_X + 2.5, 0, 50.5, yawOf(Direction.EAST), NpcRole.SOLDIER, "Рядовой Костя", 0, false);
        npc(TRENCH_X + 2.5, 0, 110.5, yawOf(Direction.EAST), NpcRole.SOLDIER, "Рядовой Айрат", 0, false);
    }

    // ------------------------------------------------------------------ the observation post in front of the wire

    private void observationPost() {
        int cx = FRONT_OP.getX() - FRONT_OX, cz = FRONT_OP.getZ() - FRONT_OZ;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                set(cx + dx, 0, cz + dz, AIR);
                set(cx + dx, -1, cz + dz, Blocks.MUD);
                set(cx + dx, 1, cz + dz, AIR);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) set(cx + dx, 1, cz + dz, sandbag);
            }
        set(cx - 2, 1, cz, AIR);   // the way back
    }

    // ------------------------------------------------------------------ the far side and the rear

    private void treeLine() {
        for (int z = 2; z < FRONT_D - 2; z += 3) {
            for (int row = 0; row < 3; row++) {
                int x = 156 + row * 5 + rnd.nextInt(3), zz = z + rnd.nextInt(2);
                if (rnd.nextInt(4) == 0) {
                    // a tree cut down by shellfire
                    fill(x, 1, zz, x, 1 + rnd.nextInt(4), zz, Blocks.STRIPPED_DARK_OAK_LOG);
                } else tree(x, zz, rnd.nextInt(3) == 0 ? Tree.BIRCH : Tree.OAK);
            }
        }
    }

    private void hamlet() {
        int[][] houses = {{6, 20}, {6, 46}, {8, 108}, {6, 132}, {22, 128}};
        for (int[] h : houses) ruin(h[0], h[1]);
    }

    /** A brick house without a roof: broken walls of random height, charred floor, rubble. */
    private void ruin(int x, int z) {
        int w = 9, d = 8;
        fill(x, 0, z, x + w, 0, z + d, Blocks.SPRUCE_PLANKS);
        for (int i = 0; i <= w; i++)
            for (int k = 0; k <= d; k++) {
                if (i != 0 && i != w && k != 0 && k != d) {
                    if (rnd.nextInt(5) == 0) set(x + i, 1, z + k, rnd.nextBoolean() ? Blocks.COBBLESTONE : Blocks.GRAVEL);
                    else if (rnd.nextInt(7) == 0) set(x + i, 0, z + k, Blocks.BLACKSTONE);
                    continue;
                }
                int h = 1 + rnd.nextInt(5);
                for (int y = 1; y <= h; y++) {
                    boolean window = y == 2 && (i % 3 == 1 || k % 3 == 1);
                    set(x + i, y, z + k, window ? AIR : (rnd.nextInt(6) == 0 ? st(Blocks.CRACKED_STONE_BRICKS) : st(Blocks.BRICKS)));
                }
            }
        for (int i = 0; i < 6; i++) set(x + rnd.nextInt(w + 4) - 2, 1, z + rnd.nextInt(d + 4) - 2, Blocks.COBBLESTONE_SLAB);
        set(x + 3, 1, z + 3, Blocks.STRIPPED_DARK_OAK_LOG);
    }

    /** The truck that brought the platoon, parked in the rear. */
    private void truck() {
        int x = 12, z = 76;
        for (int wx : new int[]{x + 1, x + 4, x + 7})
            for (int wz : new int[]{z, z + 4})
                set(wx, 1, wz, Blocks.BLACK_CONCRETE);
        fill(x, 2, z, x + 8, 2, z + 4, Blocks.GREEN_TERRACOTTA);
        for (int k = 0; k <= 5; k++) {
            set(x + k, 3, z, Blocks.GREEN_WOOL);
            set(x + k, 3, z + 4, Blocks.GREEN_WOOL);
            set(x + k, 4, z, Blocks.GREEN_WOOL);
            set(x + k, 4, z + 4, Blocks.GREEN_WOOL);
            fill(x + k, 5, z, x + k, 5, z + 4, Blocks.GREEN_WOOL);
        }
        fill(x + 6, 3, z, x + 8, 4, z + 4, Blocks.GREEN_TERRACOTTA);
        fill(x + 8, 4, z + 1, x + 8, 4, z + 3, Blocks.GLASS_PANE);
        // the way from the truck to the dugout: a communication trench
        for (int cx = x + 10; cx < 36; cx++)
            for (int cz = 79; cz <= 81; cz++) {
                set(cx, -1, cz, Blocks.SPRUCE_PLANKS);
                set(cx, 0, cz, AIR);
                set(cx, 1, cz, AIR);
            }
    }
}
