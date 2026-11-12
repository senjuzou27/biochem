package ru.ufamod;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import ru.ufamod.dev.AutoTest;
import ru.ufamod.dev.AutoTestClient;
import ru.ufamod.net.Net;
import ru.ufamod.registry.ModEntities;
import ru.ufamod.registry.ModItems;

/** "Ufa: Exam Day" - a story mod about one day of a medical student in the city of Ufa. */
@Mod(UfaMod.MODID)
public class UfaMod {
    public static final String MODID = "ufamod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UfaMod(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        Net.register();
        if (AutoTest.ENABLED) {
            MinecraftForge.EVENT_BUS.register(new AutoTest());
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(new AutoTestClient()));
        }
    }
}
