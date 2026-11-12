package ru.ufamod.dev;

import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.ufamod.UfaMod;
import ru.ufamod.building.Model;
import ru.ufamod.city.BuildingInfo;
import ru.ufamod.city.CityData;
import ru.ufamod.world.Places;
import ru.ufamod.world.Stamps;
import ru.ufamod.world.Story;

/**
 * Developer smoke test (gradlew runServer -PufaExit): generates every building of the map once,
 * reports failures and timing, resolves the story places and stops the server.
 */
public final class DevHooks {
    public static final boolean ENABLED = Boolean.getBoolean("ufamod.exitAfterBuild");

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CityData city = CityData.get();
        long t0 = System.currentTimeMillis();
        int ok = 0, empty = 0, failed = 0;
        long volume = 0;
        for (BuildingInfo b : city.buildings) {
            try {
                Model m = Story.build(city, b);
                if (m == null) empty++;
                else {
                    ok++;
                    volume += m.volume();
                }
            } catch (Throwable t) {
                failed++;
                if (failed <= 5) UfaMod.LOGGER.error("DEVTEST building {} {} failed", b.index, b.address(), t);
            }
        }
        UfaMod.LOGGER.info("DEVTEST buildings: {} ok, {} skipped, {} FAILED, {} ms, {} M cells", ok, empty, failed, System.currentTimeMillis() - t0, volume / 1_000_000);
        Places p = Places.get();
        UfaMod.LOGGER.info("DEVTEST places: wake {} bed {} dormOut {} busFrom {} busTo {} bsmuDoor {} guard {} lecture {} exam {} examOut {} library {} canteen {} salavat {}",
                p.wake, p.bed, p.dormOut, p.busFrom, p.busTo, p.bsmuDoor, p.guard, p.lecture, p.exam, p.examOut, p.library, p.canteen, p.salavat);
        UfaMod.LOGGER.info("DEVTEST stamps: {}", Stamps.all().size());
        event.getServer().halt(false);
    }
}
