package ru.ufamod.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.ufamod.UfaMod;
import ru.ufamod.block.TrafficLightBlock;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, UfaMod.MODID);

    public static final RegistryObject<Block> TRAFFIC_LIGHT = BLOCKS.register("traffic_light",
            () -> new TrafficLightBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2f).sound(SoundType.METAL)
                    .noOcclusion().lightLevel(s -> 9)));
}
