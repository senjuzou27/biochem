package ru.ufamod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import ru.ufamod.UfaMod;
import ru.ufamod.quest.Loot;

import java.util.ArrayList;
import java.util.List;

/** Writes a {@link Canvas} into the world. */
public final class CityPaster {
    private CityPaster() {}

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public static int paste(ServerLevel level, Canvas c) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        List<BlockPos> shaped = new ArrayList<>();
        int count = 0;
        for (int x = 0; x < c.sx; x++)
            for (int z = 0; z < c.sz; z++)
                for (int y = Canvas.MIN_Y; y < Canvas.MIN_Y + Canvas.SY; y++) {
                    BlockState s = c.get(x, y, z);
                    if (s == null) continue;
                    p.set(c.worldX(x), c.worldY(y), c.worldZ(z));
                    level.setBlock(p, s, FLAGS);
                    count++;
                    Block b = s.getBlock();
                    if (b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock || b instanceof FenceGateBlock)
                        shaped.add(p.immutable());
                }
        // Fences, panes, walls and stairs need to learn about their neighbours.
        for (BlockPos q : shaped) {
            BlockState cur = level.getBlockState(q);
            BlockState fixed = Block.updateFromNeighbourShapes(cur, level, q);
            if (fixed != cur) level.setBlock(q, fixed, FLAGS);
        }
        for (Canvas.SignSpec s : c.signs) {
            BlockPos q = new BlockPos(c.worldX(s.x()), c.worldY(s.y()), c.worldZ(s.z()));
            if (level.getBlockEntity(q) instanceof SignBlockEntity sign) {
                SignText text = new SignText();
                for (int i = 0; i < 4; i++) {
                    String line = i < s.lines().length && s.lines()[i] != null ? s.lines()[i] : "";
                    text = text.setMessage(i, Component.literal(line));
                }
                sign.setText(text, true);
                sign.setText(text, false);
                sign.setWaxed(true);
                sign.setChanged();
            } else {
                UfaMod.LOGGER.warn("No sign block entity at {} for '{}'", q, String.join(" ", s.lines()));
            }
        }
        for (Canvas.ChestSpec s : c.chests) {
            BlockPos q = new BlockPos(c.worldX(s.x()), c.worldY(s.y()), c.worldZ(s.z()));
            BlockEntity be = level.getBlockEntity(q);
            if (be instanceof Container container) {
                List<ItemStack> items = Loot.of(s.loot());
                for (int i = 0; i < items.size() && i < container.getContainerSize(); i++) container.setItem(i, items.get(i));
                be.setChanged();
            }
        }
        for (int[] l : c.lecterns) {
            BlockPos q = new BlockPos(c.worldX(l[0]), c.worldY(l[1]), c.worldZ(l[2]));
            if (level.getBlockEntity(q) instanceof LecternBlockEntity lectern) {
                lectern.setBook(Loot.atlas());
                lectern.setChanged();
            }
        }
        return count;
    }
}
