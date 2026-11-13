package ru.ufamod.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.ufamod.UfaMod;
import ru.ufamod.city.CityData;
import ru.ufamod.city.Ground;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.quest.Loot;
import ru.ufamod.quest.QuestManager;
import ru.ufamod.quest.Stage;
import ru.ufamod.registry.ModItems;
import ru.ufamod.world.Places;
import ru.ufamod.world.Plan;
import ru.ufamod.world.UfaWorld;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Developer self-test, enabled with -Dufamod.autotest=true (gradlew runClient -PufaAuto).
 * It plays the whole story with the first player that joins - the bad ending, then the good one -
 * checks the stage after every step and asks the client to take screenshots along the way.
 */
public final class AutoTest {
    public static final boolean ENABLED = Boolean.getBoolean("ufamod.autotest");

    /** Set by the server script, consumed by the client. */
    public static volatile String pendingShot;
    /** "fail" or "pass": how the client should answer the exam; "show" leaves the screen open. */
    public static volatile String examMode;
    public static volatile boolean finished;

    private record Step(int delay, String name, Runnable action) {}

    private final Deque<Step> steps = new ArrayDeque<>();
    private ServerPlayer player;
    private int wait = -1;
    private int failures;

    private void step(int delay, String name, Runnable action) {
        steps.add(new Step(delay, name, action));
    }

    private void shot(String name) {
        step(200, "screenshot " + name, () -> pendingShot = name);
    }

    private void expect(Stage stage) {
        step(5, "expect " + stage, () -> {
            Stage actual = QuestManager.stage(player);
            if (actual != stage) {
                failures++;
                UfaMod.LOGGER.error("AUTOTEST FAIL: expected stage {} but was {}", stage, actual);
            } else UfaMod.LOGGER.info("AUTOTEST ok: stage {}", stage);
        });
    }

    private void tp(double x, double y, double z, float yaw, float pitch) {
        step(20, "tp " + (int) x + " " + (int) y + " " + (int) z, () -> {
            ServerLevel level = UfaWorld.level(player.server);
            player.teleportTo(level, x, y, z, yaw, pitch);
        });
    }

    /** Teleports into the air and keeps the (creative) player hovering there. */
    private void fly(double x, double y, double z, float yaw, float pitch) {
        step(20, "fly " + (int) x + " " + (int) y + " " + (int) z, () -> {
            ServerLevel level = UfaWorld.level(player.server);
            player.teleportTo(level, x, y, z, yaw, pitch);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
    }

    private void tp(BlockPos p, float yaw, float pitch) {
        tp(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, pitch);
    }

    /** Stands at {@code from} looking at {@code at}. */
    private void view(BlockPos from, BlockPos at, float pitch) {
        float yaw = (float) (Mth.atan2(at.getZ() - from.getZ(), at.getX() - from.getX()) * 180.0 / Math.PI) - 90f;
        tp(from, yaw, pitch);
    }

    /** The nearest piece of road or pavement to (x, z). */
    private static BlockPos street(int x, int z) {
        CityData c = CityData.get();
        for (int rad = 0; rad < 40; rad++)
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
                    int g = c.ground(x + dx, z + dz);
                    if ((g == Ground.ROAD || g == Ground.SIDEWALK || g == Ground.PLAZA) && c.building(x + dx, z + dz) == 0)
                        return new BlockPos(x + dx, c.height(x + dx, z + dz) + 2, z + dz);
                }
        return new BlockPos(x, c.height(x, z) + 2, z);
    }

    private void talk(NpcRole role) {
        step(60, "talk to " + role, () -> {
            List<NpcEntity> near = player.level().getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(16), n -> n.getRole() == role);
            if (near.isEmpty()) {
                failures++;
                UfaMod.LOGGER.error("AUTOTEST FAIL: no NPC with role {} near {}", role, player.blockPosition());
            } else {
                near.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
                QuestManager.onTalk(player, near.get(0));
            }
        });
    }

    private void script() {
        Places P = Places.get();
        double cx = Plan.CAMP_OX, cz = Plan.CAMP_OZ;
        int cy = Plan.BASE_Y + 1;
        UfaMod.LOGGER.info("AUTOTEST places: wake {} dormOut {} bsmuDoor {} guard {} lecture {} exam {} examOut {} library {} canteen {} salavat {}",
                P.wake, P.dormOut, P.bsmuDoor, P.guard, P.lecture, P.exam, P.examOut, P.library, P.canteen, P.salavat);
        expect(Stage.WAKE);
        view(P.wake, P.bed, 15);
        shot("01_dorm_room");
        step(10, "take coat and record book", () -> {
            player.getInventory().add(new ItemStack(ModItems.LAB_COAT.get()));
            player.getInventory().add(new ItemStack(ModItems.RECORD_BOOK.get()));
            player.getInventory().add(Loot.atlas());
        });
        step(20, "wait", () -> { });
        expect(Stage.BREAKFAST);
        step(10, "eat", () -> {
            ItemStack food = new ItemStack(ModItems.ECHPOCHMAK.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, food);
            ItemStack rest = food.finishUsingItem(player.level(), player);
            net.minecraftforge.event.ForgeEventFactory.onItemUseFinish(player, new ItemStack(ModItems.ECHPOCHMAK.get()), 0, rest);
        });
        expect(Stage.TO_BSMU);
        view(P.dormLobby, P.wake, -20);
        shot("02_dorm_staircase");
        view(P.dormView, P.wake, -20);
        shot("03_dormitory");
        tp(P.dormOut, 0, 0);
        talk(NpcRole.DRIVER);
        step(60, "ride", () -> { });
        expect(Stage.WATCHMAN);
        view(P.bsmuView, P.guard, -18);
        shot("04_bsmu_building_7");
        view(P.mainView, P.mainDoor, -14);
        shot("04_bsmu_main_building");
        view(P.mainDoor, P.mainDoor.offset(-6, 3, 0), -20);
        shot("04_bsmu_main_entrance");
        view(P.bsmuHall, P.guard, 0);
        talk(NpcRole.WATCHMAN);
        expect(Stage.WATCHMAN);
        step(10, "put on the coat", () -> {
            player.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.LAB_COAT.get()), -1, player.inventoryMenu.getCraftSlots());
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.LAB_COAT.get()));
        });
        shot("05_bsmu_hall");
        talk(NpcRole.WATCHMAN);
        expect(Stage.LECTURE);
        view(P.lectureRoom, P.lecture, 5);
        shot("06_biochemistry_class");
        talk(NpcRole.LECTURER);
        expect(Stage.LECTURE_RUNNING);
        step(520, "listen to the lecture", () -> { });
        expect(Stage.TO_EXAM);
        view(P.mainView.offset(4, 0, 40), P.library, -15);
        shot("07_national_library");
        view(P.examOut.offset(0, 0, 0), P.exam, -15);
        shot("09_anatomy_department");
        view(P.examHall, P.exam, 5);
        shot("10_anatomy_inside");
        step(10, "exam mode show", () -> examMode = "show");
        talk(NpcRole.PROFESSOR);
        expect(Stage.EXAM_RUNNING);
        shot("11_exam_screen");
        step(20, "answer wrong", () -> examMode = "fail");
        step(60, "wait", () -> { });
        expect(Stage.FAILED);
        view(P.examOut, P.exam, 0);
        step(40, "wait", () -> { });
        expect(Stage.ARREST);
        step(10, "turn around", () -> player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot() + 180f, 0));
        shot("12_arrest");
        step(200, "wait for the camp", () -> { });
        expect(Stage.CAMP_REPORT);
        tp(cx + 30.5, cy, cz + 80.5, -90, 0);
        shot("13_camp");
        talk(NpcRole.SERGEANT);
        expect(Stage.CAMP_COURSE);
        tp(cx + 145.5, cy, cz + 27.5, 90, 0);
        step(30, "wait", () -> { });
        expect(Stage.CAMP_RANGE);
        tp(cx + 70.5, cy, cz + 64.5, -90, 0);
        for (int i = 0; i < 5; i++) {
            final int n = i;
            step(25, "shoot target " + n, () -> {
                int tx = 98 + (n % 2) * 8, tz = 58 + n * 3;
                Arrow arrow = new Arrow(player.level(), player);
                arrow.setPos(cx + tx - 4, cy + 1.5, cz + tz + 0.5);
                arrow.shoot(1, 0, 0, 2.5f, 0);
                player.level().addFreshEntity(arrow);
            });
        }
        step(40, "wait", () -> { });
        expect(Stage.CAMP_TRENCH);
        tp(cx + 100.5, cy, cz + 112.5, 90, 20);
        step(10, "dig", () -> {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                if (player.getInventory().getItem(i).is(ModItems.SAPPER_SHOVEL.get())) {
                    ItemStack shovel = player.getInventory().getItem(i);
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
                    break;
                }
            for (int i = 0; i < 12; i++) {
                BlockPos pos = new BlockPos((int) cx + 96 + i % 4, cy - 1, (int) cz + 114 + i / 4);
                if (!player.gameMode.destroyBlock(pos)) UfaMod.LOGGER.error("AUTOTEST FAIL: could not dig {} ({})", pos, player.level().getBlockState(pos));
            }
        });
        expect(Stage.CAMP_TRUCK);
        tp(cx + 16.5, cy + 2, cz + 124.5, 90, 0);
        step(30, "wait", () -> { });
        expect(Stage.END_SVO);

        // ---- second run: the good ending
        step(20, "restart", () -> QuestManager.start(player));
        expect(Stage.WAKE);
        step(10, "skip to the exam", () -> QuestManager.forceStage(player, Stage.TO_EXAM));
        tp(P.examHall, 0, 0);
        step(10, "exam mode pass", () -> examMode = "pass");
        talk(NpcRole.PROFESSOR);
        step(80, "wait", () -> { });
        expect(Stage.PASSED);
        tp(P.bsmuDoor, 0, 0);
        talk(NpcRole.DRIVER);
        step(60, "ride", () -> { });
        expect(Stage.FREE);
        step(10, "walk freely", () -> {
            QuestManager.free(player);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        });
        view(P.salavat.offset(0, 0, -6), P.salavat.offset(0, 6, 12), -18);
        shot("14_salavat_yulaev");
        view(P.salavat.offset(14, 0, 30), P.salavat.offset(0, 8, 12), -20);
        shot("15_salavat_from_the_river_side");
        view(P.friendship.offset(0, 0, -8), P.friendship.offset(0, 14, 11), -30);
        shot("16_friendship_monument");
        BlockPos gostiny = street(P.gostiny.getX(), P.gostiny.getZ());
        fly(gostiny.getX() + 0.5, gostiny.getY() + 14, gostiny.getZ() + 0.5, 90, 15);
        shot("17_gostiny_dvor");
        tp(gostiny, -90, -5);
        shot("18_lenina_near_gostiny");
        BlockPos operaView = street(P.opera.getX() + 70, P.opera.getZ() - 40);
        float operaYaw = (float) (Mth.atan2(P.opera.getZ() - 40 - operaView.getZ(), P.opera.getX() - operaView.getX()) * 180.0 / Math.PI) - 90f;
        fly(operaView.getX() + 0.5, operaView.getY() + 12, operaView.getZ() + 0.5, operaYaw, 8);
        shot("19_opera_theatre");
        tp(street(P.park.getX(), P.park.getZ() + 40), 180, 0);
        shot("20_lenin_park");
        tp(street(33, -713), 180, -8);
        shot("21_lenina_street_north");
        tp(street(33, -713), 90, -8);
        shot("22_lenina_street_west_side");
        tp(street(-600, -300), 0, -10);
        shot("23_old_quarter");
        tp(street(900, -900), 45, -12);
        shot("24_residential_quarter");
        tp(street(1500, -1200), 200, -12);
        shot("25_dorm_district");
        tp(street(700, 300), 135, -10);
        shot("26_wooden_houses");
        // high views
        fly(P.mainView.getX() + 36.5, P.mainDoor.getY() + 45, P.mainView.getZ() + 0.5, 90, 35);
        shot("27_bsmu_from_above");
        fly(P.gostiny.getX() + 0.5, P.gostiny.getY() + 80, P.gostiny.getZ() + 60.5, 180, 50);
        shot("28_centre_from_above");
        fly(P.salavat.getX() + 0.5, P.salavat.getY() + 50, P.salavat.getZ() - 70.5, 0, 30);
        shot("29_river_bank_from_above");
        fly(P.dormView.getX() + 0.5, P.dormView.getY() + 60, P.dormView.getZ() + 40.5, 180, 40);
        shot("30_dorm_district_from_above");
        step(100, "done", () -> {
            UfaMod.LOGGER.info("AUTOTEST FINISHED with {} failure(s)", failures);
            finished = true;
        });
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (player == null) {
            List<ServerPlayer> players = event.getServer().getPlayerList().getPlayers();
            if (players.isEmpty()) return;
            player = players.get(0);
            try {
                script();
            } catch (Throwable t) {
                failures++;
                UfaMod.LOGGER.error("AUTOTEST FAIL: could not prepare the script", t);
            }
            wait = 300; // let the client finish loading the world
            UfaMod.LOGGER.info("AUTOTEST started for {}", player.getGameProfile().getName());
            return;
        }
        if (pendingShot != null) return; // the client has not taken the picture yet
        if (--wait > 0) return;
        Step next = steps.poll();
        if (next == null) return;
        try {
            UfaMod.LOGGER.info("AUTOTEST step: {}", next.name());
            next.action().run();
        } catch (Throwable t) {
            failures++;
            UfaMod.LOGGER.error("AUTOTEST FAIL in step '" + next.name() + "'", t);
        }
        Step after = steps.peek();
        wait = after == null ? 0 : after.delay();
    }
}
