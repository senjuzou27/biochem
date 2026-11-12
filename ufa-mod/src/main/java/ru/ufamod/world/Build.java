package ru.ufamod.world;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import ru.ufamod.entity.NpcRole;

import java.util.Random;

/** Base class with drawing helpers shared by all builders. */
public abstract class Build {
    protected final Canvas c;
    protected final Random rnd;

    protected static final Direction N = Direction.NORTH, S = Direction.SOUTH, E = Direction.EAST, W = Direction.WEST;
    protected static final BlockState AIR = Blocks.AIR.defaultBlockState();

    protected Build(Canvas c, long seed) {
        this.c = c;
        this.rnd = new Random(seed);
    }

    protected static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    protected void set(int x, int y, int z, BlockState s) {
        c.set(x, y, z, s);
    }

    protected void set(int x, int y, int z, Block b) {
        c.set(x, y, z, b.defaultBlockState());
    }

    protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        int ax = Math.min(x1, x2), bx = Math.max(x1, x2);
        int ay = Math.min(y1, y2), by = Math.max(y1, y2);
        int az = Math.min(z1, z2), bz = Math.max(z1, z2);
        for (int y = ay; y <= by; y++)
            for (int z = az; z <= bz; z++)
                for (int x = ax; x <= bx; x++)
                    c.set(x, y, z, s);
    }

    protected void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
    }

    protected void air(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, AIR);
    }

    /** Four vertical walls of a box (no floor, no ceiling). */
    protected void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        fill(x1, y1, z1, x2, y2, z1, s);
        fill(x1, y1, z2, x2, y2, z2, s);
        fill(x1, y1, z1, x1, y2, z2, s);
        fill(x2, y1, z1, x2, y2, z2, s);
    }

    protected void walls(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        walls(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
    }

    /** Fills only cells where nothing has been drawn yet. */
    protected void fillEmpty(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                    if (c.get(x, y, z) == null) c.set(x, y, z, s);
    }

    protected static BlockState stair(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    protected static BlockState stairTop(Block b, Direction facing) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, Half.TOP);
    }

    protected static BlockState slabTop(Block b) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
    }

    protected static BlockState slab(Block b) {
        return b.defaultBlockState();
    }

    protected static BlockState trapdoor(Block b, Direction facing, boolean top, boolean open) {
        return b.defaultBlockState().setValue(TrapDoorBlock.FACING, facing)
                .setValue(TrapDoorBlock.HALF, top ? Half.TOP : Half.BOTTOM).setValue(TrapDoorBlock.OPEN, open);
    }

    protected static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A door whose closed panel lies on the side opposite to {@code facing}. */
    protected void door(int x, int y, int z, Direction facing, Block door, boolean rightHinge) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, rightHinge ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Double door occupying two cells; (x,z) and the next cell along the wall. */
    protected void doubleDoor(int x, int y, int z, Direction facing, Block door) {
        Direction side = facing.getClockWise();
        door(x, y, z, facing, door, false);
        door(x + side.getStepX(), y, z + side.getStepZ(), facing, door, true);
    }

    /** Wall sign at (x,y,z) whose text faces {@code facing}; it hangs on the block behind it. */
    protected void sign(int x, int y, int z, Direction facing, String... lines) {
        set(x, y, z, Blocks.BIRCH_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing));
        c.signs.add(new Canvas.SignSpec(x, y, z, lines));
    }

    /** Free-standing sign; rotation 0 = faces south, 4 = west, 8 = north, 12 = east. */
    protected void post(int x, int y, int z, int rotation, String... lines) {
        set(x, y, z, Blocks.BIRCH_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation));
        c.signs.add(new Canvas.SignSpec(x, y, z, lines));
    }

    protected static int rot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            default -> 12;
        };
    }

    protected void chest(int x, int y, int z, Direction facing, String loot) {
        set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        c.chests.add(new Canvas.ChestSpec(x, y, z, loot));
    }

    /** Bed with its foot at (x,z) and its head one block further in {@code facing}. */
    protected void bed(int x, int y, int z, Direction facing, Block bed) {
        BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, facing);
        set(x, y, z, s.setValue(BedBlock.PART, BedPart.FOOT));
        set(x + facing.getStepX(), y, z + facing.getStepZ(), s.setValue(BedBlock.PART, BedPart.HEAD));
    }

    protected void npc(double x, int y, double z, float yaw, NpcRole role, String name, int variant, boolean wander) {
        c.npcs.add(new Canvas.NpcSpec(x + 0.5, y, z + 0.5, yaw, role, name, variant, wander));
    }

    protected static float yawOf(Direction d) {
        return switch (d) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            default -> -90f;
        };
    }

    // ---------------------------------------------------------------- street furniture

    protected void lamp(int x, int z) {
        fill(x, 1, z, x, 4, z, Blocks.POLISHED_BLACKSTONE_WALL);
        set(x, 5, z, Blocks.LANTERN);
    }

    protected void hangingLamp(int x, int y, int z) {
        set(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /** A bench two blocks long; {@code facing} is the direction a seated person looks. */
    protected void bench(int x, int z, Direction facing) {
        Direction side = facing.getClockWise();
        BlockState s = stair(Blocks.SPRUCE_STAIRS, facing.getOpposite());
        set(x, 1, z, s);
        set(x + side.getStepX(), 1, z + side.getStepZ(), s);
    }

    public enum Tree { OAK, BIRCH, SPRUCE, LINDEN }

    protected void tree(int x, int z, Tree kind) {
        tree(x, 0, z, kind);
    }

    protected void tree(int x, int ground, int z, Tree kind) {
        if (kind == Tree.SPRUCE) {
            int h = 7 + rnd.nextInt(3);
            fill(x, ground + 1, z, x, ground + h, z, Blocks.SPRUCE_LOG);
            BlockState l = leaves(Blocks.SPRUCE_LEAVES);
            for (int y = ground + 3; y <= ground + h + 1; y++) {
                int fromTop = ground + h + 1 - y;
                int r = fromTop == 0 ? 0 : (fromTop % 2 == 1 ? 1 + fromTop / 4 : 2 + fromTop / 4);
                if (r > 3) r = 3;
                for (int dx = -r; dx <= r; dx++)
                    for (int dz = -r; dz <= r; dz++) {
                        if (r > 1 && Math.abs(dx) == r && Math.abs(dz) == r) continue;
                        if (dx == 0 && dz == 0 && y <= ground + h) continue;
                        if (c.isEmpty(x + dx, y, z + dz)) set(x + dx, y, z + dz, l);
                    }
            }
            return;
        }
        Block log = kind == Tree.BIRCH ? Blocks.BIRCH_LOG : (kind == Tree.LINDEN ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG);
        BlockState l = leaves(kind == Tree.BIRCH ? Blocks.BIRCH_LEAVES : (kind == Tree.LINDEN ? Blocks.DARK_OAK_LEAVES : Blocks.OAK_LEAVES));
        int h = (kind == Tree.BIRCH ? 6 : 5) + rnd.nextInt(2);
        fill(x, ground + 1, z, x, ground + h, z, log);
        for (int y = ground + h - 2; y <= ground + h + 1; y++) {
            int r = y >= ground + h ? 1 : 2;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && y <= ground + h) continue;
                    if (Math.abs(dx) == r && Math.abs(dz) == r && (r == 2 || y == ground + h + 1) && rnd.nextInt(3) != 0) continue;
                    if (c.isEmpty(x + dx, y, z + dz)) set(x + dx, y, z + dz, l);
                }
        }
    }

    private static final Block[] CAR_COLORS = {Blocks.RED_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.CYAN_CONCRETE};

    /** A small parked car, 4 blocks long. */
    protected void car(int x, int z, boolean alongX, int color) {
        Block body = CAR_COLORS[Math.floorMod(color, CAR_COLORS.length)];
        for (int i = 0; i < 4; i++) {
            int cx = alongX ? x + i : x, cz = alongX ? z : z + i;
            int dx = alongX ? 0 : 1, dz = alongX ? 1 : 0;
            boolean wheel = i == 0 || i == 3;
            set(cx, 1, cz, wheel ? Blocks.BLACK_TERRACOTTA : body);
            set(cx + dx, 1, cz + dz, wheel ? Blocks.BLACK_TERRACOTTA : body);
            if (i == 1 || i == 2) {
                set(cx, 2, cz, Blocks.LIGHT_BLUE_STAINED_GLASS);
                set(cx + dx, 2, cz + dz, Blocks.LIGHT_BLUE_STAINED_GLASS);
            } else {
                set(cx, 2, cz, slab(slabOf(body)));
                set(cx + dx, 2, cz + dz, slab(slabOf(body)));
            }
        }
    }

    private static Block slabOf(Block concrete) {
        if (concrete == Blocks.WHITE_CONCRETE || concrete == Blocks.LIGHT_GRAY_CONCRETE) return Blocks.SMOOTH_QUARTZ_SLAB;
        if (concrete == Blocks.RED_CONCRETE) return Blocks.RED_NETHER_BRICK_SLAB;
        if (concrete == Blocks.BLACK_CONCRETE) return Blocks.POLISHED_BLACKSTONE_SLAB;
        if (concrete == Blocks.YELLOW_CONCRETE) return Blocks.SMOOTH_SANDSTONE_SLAB;
        if (concrete == Blocks.GREEN_CONCRETE) return Blocks.PRISMARINE_BRICK_SLAB;
        return Blocks.DARK_PRISMARINE_SLAB;
    }

    /** Flag of three horizontal stripes on a pole. */
    protected void flag(int x, int z, int height, Block top, Block mid, Block bottom, boolean alongX) {
        fill(x, 1, z, x, height, z, Blocks.IRON_BARS);
        for (int i = 1; i <= 4; i++) {
            int fx = alongX ? x + i : x, fz = alongX ? z : z + i;
            set(fx, height, fz, top);
            set(fx, height - 1, fz, mid);
            set(fx, height - 2, fz, bottom);
        }
    }

    // ---------------------------------------------------------------- roofs

    /** Gable roof whose ridge runs along the longer side of the rectangle. */
    protected void gableRoof(int x1, int z1, int x2, int z2, int y, Block stairs, Block full) {
        boolean ridgeAlongX = (x2 - x1) >= (z2 - z1);
        int half = ((ridgeAlongX ? z2 - z1 : x2 - x1) + 2) / 2;
        for (int i = 0; i <= half; i++) {
            if (ridgeAlongX) {
                int za = z1 - 1 + i, zb = z2 + 1 - i;
                if (za > zb) break;
                if (za == zb) {
                    fill(x1 - 1, y + i, za, x2 + 1, y + i, za, slab(slabFor(full)));
                    break;
                }
                fill(x1 - 1, y + i, za, x2 + 1, y + i, za, stair(stairs, S));
                fill(x1 - 1, y + i, zb, x2 + 1, y + i, zb, stair(stairs, N));
                if (zb - za > 1) {
                    fill(x1, y + i, za + 1, x1, y + i, zb - 1, full);
                    fill(x2, y + i, za + 1, x2, y + i, zb - 1, full);
                }
            } else {
                int xa = x1 - 1 + i, xb = x2 + 1 - i;
                if (xa > xb) break;
                if (xa == xb) {
                    fill(xa, y + i, z1 - 1, xa, y + i, z2 + 1, slab(slabFor(full)));
                    break;
                }
                fill(xa, y + i, z1 - 1, xa, y + i, z2 + 1, stair(stairs, E));
                fill(xb, y + i, z1 - 1, xb, y + i, z2 + 1, stair(stairs, W));
                if (xb - xa > 1) {
                    fill(xa + 1, y + i, z1, xb - 1, y + i, z1, full);
                    fill(xa + 1, y + i, z2, xb - 1, y + i, z2, full);
                }
            }
        }
    }

    /** Hip roof: slopes on all four sides. */
    protected void hipRoof(int x1, int z1, int x2, int z2, int y, Block stairs, Block full) {
        int xa = x1 - 1, xb = x2 + 1, za = z1 - 1, zb = z2 + 1;
        int i = 0;
        while (xa <= xb && za <= zb && i < 6) {
            if (xa == xb || za == zb) {
                fill(xa, y + i, za, xb, y + i, zb, slab(slabFor(full)));
                return;
            }
            fill(xa, y + i, za, xb, y + i, za, stair(stairs, S));
            fill(xa, y + i, zb, xb, y + i, zb, stair(stairs, N));
            fill(xa, y + i, za + 1, xa, y + i, zb - 1, stair(stairs, E));
            fill(xb, y + i, za + 1, xb, y + i, zb - 1, stair(stairs, W));
            xa++;
            xb--;
            za++;
            zb--;
            i++;
        }
        if (xa <= xb && za <= zb) fill(xa, y + i - 1, za, xb, y + i - 1, zb, full);
    }

    private static Block slabFor(Block full) {
        if (full == Blocks.BRICKS) return Blocks.BRICK_SLAB;
        if (full == Blocks.SPRUCE_PLANKS) return Blocks.SPRUCE_SLAB;
        if (full == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_SLAB;
        if (full == Blocks.DEEPSLATE_TILES) return Blocks.DEEPSLATE_TILE_SLAB;
        if (full == Blocks.RED_NETHER_BRICKS) return Blocks.RED_NETHER_BRICK_SLAB;
        if (full == Blocks.STONE_BRICKS) return Blocks.STONE_BRICK_SLAB;
        if (full == Blocks.DARK_PRISMARINE) return Blocks.DARK_PRISMARINE_SLAB;
        if (full == Blocks.CUT_COPPER) return Blocks.CUT_COPPER_SLAB;
        return Blocks.SMOOTH_STONE_SLAB;
    }
}
