package ru.ufamod.dev;

import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.ufamod.UfaMod;
import net.minecraft.core.BlockPos;
import ru.ufamod.building.BuildingGenerator;
import ru.ufamod.building.WalkTest;
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
        int multi = 0, noStairs = 0, topOk = 0, topJump = 0, topStuck = 0, groundStuck = 0;
        long volume = 0;
        StringBuilder stuck = new StringBuilder();
        for (BuildingInfo b : city.buildings) {
            try {
                BuildingGenerator g = Story.generate(city, b);
                if (g == null) {
                    empty++;
                    continue;
                }
                ok++;
                volume += g.m.volume();
                // can the ground floor and the top floor be left on foot?
                BlockPos ground = null, top = null;
                for (BuildingGenerator.RoomRef r : g.rooms) {
                    BlockPos c = BuildingGenerator.roomCell(r, r.width() / 2, r.depth() - 1);
                    if (r.storey() == 0 && ground == null) ground = c;
                    if (r.storey() == g.levels - 1 && top == null) top = c;
                }
                if (ground != null && !WalkTest.canLeave(city, g, ground, true)) {
                    groundStuck++;
                    if (groundStuck <= 6) stuck.append(" ground:").append(b.index).append('@').append(ground.toShortString());
                }
                if (g.levels > 1 && top != null) {
                    multi++;
                    if (WalkTest.canLeave(city, g, top, false)) topOk++;
                    else if (WalkTest.canLeave(city, g, top, true)) topJump++;
                    else {
                        topStuck++;
                        if (topStuck <= 6) stuck.append(" top:").append(b.index).append('@').append(top.toShortString()).append(" lv").append(g.levels);
                    }
                }
            } catch (Throwable t) {
                failed++;
                if (failed <= 5) UfaMod.LOGGER.error("DEVTEST building {} {} failed", b.index, b.address(), t);
            }
        }
        UfaMod.LOGGER.info("DEVTEST buildings: {} ok, {} skipped, {} FAILED, {} ms, {} M cells", ok, empty, failed, System.currentTimeMillis() - t0, volume / 1_000_000);
        UfaMod.LOGGER.info("DEVTEST walking: ground floor stuck {}; of {} multi-storey buildings the top floor is reachable by stairs in {}, only by jumping in {}, not at all in {}",
                groundStuck, multi, topOk, topJump, topStuck);
        UfaMod.LOGGER.info("DEVTEST stuck examples:{}", stuck);
        for (int index : new int[]{city.role("dorm").index}) {
            BuildingGenerator g = Story.generate(city, city.buildingInfo(index));
            if (g == null || Story.MARKS.get("wake") == null) continue;
            BlockPos start = null;
            for (BuildingGenerator.RoomRef r : g.rooms) if (r.storey() == 1 && start == null) start = BuildingGenerator.roomCell(r, r.width() / 2, r.depth() - 1);
            if (index == city.role("dorm").index) start = Story.MARKS.get("wake");
            java.util.Set<Long> seen = new java.util.HashSet<>();
            boolean out = WalkTest.canLeave(city, g, start, false, seen);
            StringBuilder sb = new StringBuilder();
            for (int z = g.fp.oz; z < g.fp.oz + g.fp.d; z++) {
                for (int x = g.fp.ox; x < g.fp.ox + g.fp.w; x++) {
                    char ch = city.building(x, z) == g.b.index ? '.' : ' ';
                    for (int dy = -4; dy <= 1; dy++) if (seen.contains(BlockPos.asLong(x, start.getY() + dy, z))) ch = dy == 0 ? '*' : dy == 1 ? '+' : (char) ('0' - dy);
                    if (x == start.getX() && z == start.getZ()) ch = '@';
                    sb.append(ch);
                }
                sb.append('\n');
            }
            UfaMod.LOGGER.info("DEVTEST map of {} storey 0 ({} {}):\n{}", index, g.st.use, g.st.era, g.debugMap(0));
            UfaMod.LOGGER.info("DEVTEST map of {} storey 1:\n{}", index, g.debugMap(1));
            UfaMod.LOGGER.info("DEVTEST reach of {} from {} -> {} ({} cells; digits = levels below the start):\n{}", index, start, out, seen.size(), sb);
        }
        for (String role : new String[]{"dorm", "bsmu_main", "bsmu_anatomy", "bsmu_canteen", "library"}) {
            BuildingInfo b = city.role(role);
            BuildingGenerator g = Story.generate(city, b);
            if (g == null) continue;
            String[] marks = switch (role) {
                case "dorm" -> new String[]{"wake"};
                case "bsmu_main" -> new String[]{"guard", "lecture"};
                case "bsmu_anatomy" -> new String[]{"exam"};
                case "bsmu_canteen" -> new String[]{"canteen"};
                default -> new String[]{"library"};
            };
            for (String mark : marks) {
                BlockPos pos = Story.MARKS.get(mark);
                if (pos == null) continue;
                UfaMod.LOGGER.info("DEVTEST story place {} in {}: way out by stairs {}, with jumps {} ({} storeys, {} entrances, {} rooms)", mark, role,
                        WalkTest.canLeave(city, g, pos, false), WalkTest.canLeave(city, g, pos, true), g.levels, g.entrances.size(), g.rooms.size());
            }
        }
        Places p = Places.get();
        UfaMod.LOGGER.info("DEVTEST places: wake {} bed {} dormOut {} busFrom {} busTo {} bsmuDoor {} guard {} lecture {} exam {} examOut {} library {} canteen {} salavat {}",
                p.wake, p.bed, p.dormOut, p.busFrom, p.busTo, p.bsmuDoor, p.guard, p.lecture, p.exam, p.examOut, p.library, p.canteen, p.salavat);
        UfaMod.LOGGER.info("DEVTEST stamps: {}", Stamps.all().size());
        event.getServer().halt(false);
    }
}
