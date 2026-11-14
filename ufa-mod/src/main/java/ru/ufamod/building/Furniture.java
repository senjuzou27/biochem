package ru.ufamod.building;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Furniture from Paladin's Furniture Mod ("pfm") when it is installed. Every method returns null when the
 * mod or the block is missing, and the building generator then uses its own vanilla stand-in.
 */
public final class Furniture {
    private Furniture() {}

    private static volatile Boolean loaded;
    private static final Map<String, Optional<BlockState>> CACHE = new ConcurrentHashMap<>();

    public static boolean present() {
        Boolean l = loaded;
        if (l == null) loaded = l = ModList.get() != null && ModList.get().isLoaded("pfm");
        return l;
    }

    /** A block of the mod with some of its state properties set by name, e.g. state("oak_chair", "facing", "north"). */
    @Nullable
    public static BlockState state(String id, String... props) {
        if (!present()) return null;
        String key = id + String.join(",", props);
        return CACHE.computeIfAbsent(key, k -> Optional.ofNullable(build(id, props))).orElse(null);
    }

    @Nullable
    private static BlockState build(String id, String... props) {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pfm", id));
        if (b == null || b == Blocks.AIR) return null;
        BlockState s = b.defaultBlockState();
        for (int i = 0; i + 1 < props.length; i += 2) s = with(s, props[i], props[i + 1]);
        return s;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, String name, String value) {
        for (Property<?> p : s.getProperties()) {
            if (!p.getName().equals(name)) continue;
            @SuppressWarnings("unchecked") Property<T> tp = (Property<T>) p;
            Optional<T> v = tp.getValue(value);
            return v.isPresent() ? s.setValue(tp, v.get()) : s;
        }
        return s;
    }

    // ------------------------------------------------------------------ the pieces the generator uses

    /** A chair whose back is on the given side; the sitter looks the other way. */
    @Nullable
    public static BlockState chair(Direction back, String kind) {
        return state(kind, "facing", back.getOpposite().getName(), "tucked", "false");
    }

    @Nullable
    public static BlockState sofa(Direction back) {
        return state("light_gray_simple_sofa", "facing", back.getOpposite().getName());
    }

    /** A piece that stands against a wall and shows its front in the given direction. */
    @Nullable
    public static BlockState facing(String id, Direction front) {
        return state(id, "facing", front.getName());
    }

    @Nullable
    public static BlockState desk(String wood) {
        return state(wood + "_desk_basic");
    }

    @Nullable
    public static BlockState table(Direction.Axis axis) {
        return state("light_wood_table_basic", "axis", axis == Direction.Axis.X ? "x" : "z");
    }

    @Nullable
    public static BlockState lamp() {
        return state("basic_lamp", "lit", "true");
    }

    @Nullable
    public static BlockState pendant() {
        return state("white_modern_pendant", "lit", "true", "up", "true");
    }

    /** The bed of the mod for a vanilla bed colour, or null. */
    @Nullable
    public static BlockState bed(Block vanillaBed) {
        String colour = ForgeRegistries.BLOCKS.getKey(vanillaBed).getPath().replace("_bed", "");
        return state("oak_" + colour + "_simple_bed");
    }
}
