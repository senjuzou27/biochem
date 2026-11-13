package ru.ufamod.dev;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import ru.ufamod.UfaMod;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Developer aid for working with other mods: with {@code -Dufamod.dumpRegistry=file} the server writes the
 * blocks (with their state properties), items and entity types of those mods to a file and stops.
 */
public final class RegistryDump {
    public static final String FILE = System.getProperty("ufamod.dumpRegistry");
    private static final Set<String> SKIP = Set.of("minecraft", "forge", UfaMod.MODID);

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) {
        try (PrintWriter out = new PrintWriter(FILE, StandardCharsets.UTF_8)) {
            ForgeRegistries.BLOCKS.getEntries().forEach(e -> {
                if (SKIP.contains(e.getKey().location().getNamespace())) return;
                Block b = e.getValue();
                StringBuilder sb = new StringBuilder("block ").append(e.getKey().location());
                for (Property<?> p : b.getStateDefinition().getProperties()) sb.append(' ').append(p.getName()).append('=').append(p.getPossibleValues());
                out.println(sb);
            });
            ForgeRegistries.ITEMS.getKeys().forEach(k -> {
                if (!SKIP.contains(k.getNamespace())) out.println("item " + k);
            });
            ForgeRegistries.ENTITY_TYPES.getEntries().forEach(e -> {
                if (!SKIP.contains(e.getKey().location().getNamespace()))
                    out.println("entity " + e.getKey().location() + " " + e.getValue().getCategory() + " " + e.getValue().getWidth() + "x" + e.getValue().getHeight());
            });
        } catch (Exception ex) {
            UfaMod.LOGGER.error("Registry dump failed", ex);
        }
        UfaMod.LOGGER.info("Registry dump written to {}", FILE);
        event.getServer().halt(false);
    }
}
