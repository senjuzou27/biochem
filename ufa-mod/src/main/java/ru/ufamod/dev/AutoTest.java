package ru.ufamod.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.quest.Loot;
import ru.ufamod.quest.QuestManager;
import ru.ufamod.quest.Stage;
import ru.ufamod.registry.ModItems;
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
    /** "fail" or "pass": how the client should answer the exam; null = a human answers. */
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
        step(160, "screenshot " + name, () -> pendingShot = name);
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

    private void tp(double x, int localY, double z, float yaw, float pitch) {
        step(20, "tp " + x + " " + localY + " " + z, () -> {
            ServerLevel level = UfaWorld.level(player.server);
            player.teleportTo(level, x, Plan.BASE_Y + localY, z, yaw, pitch);
        });
    }

    private void talk(NpcRole role) {
        step(60, "talk to " + role, () -> {
            List<NpcEntity> near = player.level().getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(14), n -> n.getRole() == role);
            if (near.isEmpty()) {
                failures++;
                UfaMod.LOGGER.error("AUTOTEST FAIL: no NPC with role {} near {}", role, player.blockPosition());
            } else QuestManager.onTalk(player, near.get(0));
        });
    }

    private void script() {
        int f2 = Plan.bsFloor(2) + 1, f3 = Plan.bsFloor(3) + 1;
        double cx = Plan.CAMP_OX, cz = Plan.CAMP_OZ;
        expect(Stage.WAKE);
        tp(355.5, 1, 26.5, 90, 10);
        shot("01_dorm_room");
        step(10, "take coat and record book", () -> {
            player.getInventory().add(new ItemStack(ModItems.LAB_COAT.get()));
            player.getInventory().add(new ItemStack(ModItems.RECORD_BOOK.get()));
            player.getInventory().add(Loot.atlas());
        });
        step(20, "wait", () -> { });
        expect(Stage.BREAKFAST);
        tp(349.5, 1, 38.5, -90, 5);
        shot("02_dorm_kitchen");
        step(10, "eat", () -> {
            ItemStack food = new ItemStack(ModItems.ECHPOCHMAK.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, food);
            ItemStack rest = food.finishUsingItem(player.level(), player);
            net.minecraftforge.event.ForgeEventFactory.onItemUseFinish(player, new ItemStack(ModItems.ECHPOCHMAK.get()), 0, rest);
        });
        expect(Stage.TO_BSMU);
        tp(337.5, 1, 44.5, 150, 0);
        shot("03_dorm_outside");
        tp(327.5, 1, 90.5, 0, 0);
        shot("04_lenina_street");
        tp(306.5, 1, 182.5, 140, -12);
        shot("05_gostiny_dvor");
        tp(248.5, 1, 134.5, -90, -15);
        shot("06_gostiny_inside");
        tp(334.5, 1, 284.5, 100, -15);
        shot("07_opera_theatre");
        tp(336.5, 1, 392.5, 80, -18);
        step(30, "wait", () -> { });
        expect(Stage.TO_BSMU);
        shot("08_bsmu_front");
        tp(318.5, 2, 386.5, 90, 0);
        step(30, "wait", () -> { });
        expect(Stage.WATCHMAN);
        tp(311.5, 2, 388.5, 60, 5);
        talk(NpcRole.WATCHMAN);
        expect(Stage.WATCHMAN);
        step(10, "put on the coat", () -> {
            player.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.LAB_COAT.get()), -1, player.inventoryMenu.getCraftSlots());
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.LAB_COAT.get()));
        });
        shot("09_bsmu_hall_guard");
        talk(NpcRole.WATCHMAN);
        expect(Stage.LECTURE);
        tp(296.5, 2, 386.5, 90, -10);
        shot("10_bsmu_stairs");
        tp(303.5, f2, 359.5, 180, -8);
        shot("11_lecture_hall");
        talk(NpcRole.LECTURER);
        expect(Stage.LECTURE_RUNNING);
        step(520, "listen to the lecture", () -> { });
        expect(Stage.TO_EXAM);
        tp(303.5, f2, 408.5, 0, 5);
        shot("12_library");
        tp(309.5, 2, 357.5, 180, 5);
        shot("13_canteen");
        tp(303.5, f3, 414.5, 0, 0);
        shot("14_anatomy_corridor");
        tp(309.5, f3, 412.5, -60, 5);
        shot("15_anatomy_museum");
        tp(303.5, f3, 421.5, 0, 5);
        shot("16_exam_room");
        step(10, "exam mode fail", () -> examMode = "show");
        talk(NpcRole.PROFESSOR);
        expect(Stage.EXAM_RUNNING);
        shot("17_exam_screen");
        step(20, "answer wrong", () -> examMode = "fail");
        step(60, "wait", () -> { });
        expect(Stage.FAILED);
        tp(316.5, 2, 386.5, -90, 0);
        step(40, "wait", () -> { });
        expect(Stage.ARREST);
        shot("18_arrest");
        step(200, "wait for the camp", () -> { });
        expect(Stage.CAMP_REPORT);
        step(10, "look", () -> player.teleportTo(player.serverLevel(), cx + 30.5, Plan.BASE_Y + 1, cz + 80.5, -90, 0));
        shot("19_camp_parade_ground");
        talk(NpcRole.SERGEANT);
        expect(Stage.CAMP_COURSE);
        step(20, "tp", () -> player.teleportTo(player.serverLevel(), cx + 64.5, Plan.BASE_Y + 1, cz + 27.5, -90, 5));
        shot("20_camp_obstacle_course");
        step(20, "tp", () -> player.teleportTo(player.serverLevel(), cx + 145.5, Plan.BASE_Y + 1, cz + 27.5, 90, 0));
        step(30, "wait", () -> { });
        expect(Stage.CAMP_RANGE);
        step(20, "tp", () -> player.teleportTo(player.serverLevel(), cx + 70.5, Plan.BASE_Y + 1, cz + 64.5, -90, 0));
        shot("21_camp_range");
        for (int i = 0; i < 5; i++) {
            final int n = i;
            step(25, "shoot target " + n, () -> {
                int tx = 98 + (n % 2) * 8, tz = 58 + n * 3;
                Arrow arrow = new Arrow(player.level(), player);
                arrow.setPos(cx + tx - 4, Plan.BASE_Y + 2.5, cz + tz + 0.5);
                arrow.shoot(1, 0, 0, 2.5f, 0);
                player.level().addFreshEntity(arrow);
            });
        }
        step(40, "wait", () -> { });
        expect(Stage.CAMP_TRENCH);
        step(20, "tp", () -> player.teleportTo(player.serverLevel(), cx + 100.5, Plan.BASE_Y + 1, cz + 112.5, 90, 20));
        step(10, "dig", () -> {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                if (player.getInventory().getItem(i).is(ModItems.SAPPER_SHOVEL.get())) {
                    ItemStack shovel = player.getInventory().getItem(i);
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
                    break;
                }
            for (int i = 0; i < 12; i++) {
                BlockPos pos = new BlockPos((int) cx + 96 + i % 4, Plan.BASE_Y, (int) cz + 114 + i / 4);
                if (!player.gameMode.destroyBlock(pos)) UfaMod.LOGGER.error("AUTOTEST FAIL: could not dig {} ({})", pos, player.level().getBlockState(pos));
            }
        });
        shot("22_camp_trench");
        expect(Stage.CAMP_TRUCK);
        step(20, "tp", () -> player.teleportTo(player.serverLevel(), cx + 7.5, Plan.BASE_Y + 1, cz + 119.5, -60, 0));
        shot("23_camp_truck");
        step(20, "board", () -> player.teleportTo(player.serverLevel(), cx + 16.5, Plan.BASE_Y + 3, cz + 124.5, 90, 0));
        step(30, "wait", () -> { });
        expect(Stage.END_SVO);
        shot("24_bad_ending");

        // ---- second run: the good ending
        step(20, "restart", () -> QuestManager.start(player));
        expect(Stage.WAKE);
        step(10, "skip to the exam", () -> QuestManager.forceStage(player, Stage.TO_EXAM));
        tp(303.5, f3, 426.5, 0, 0);
        step(10, "exam mode pass", () -> examMode = "pass");
        talk(NpcRole.PROFESSOR);
        step(80, "wait", () -> { });
        expect(Stage.PASSED);
        tp(70.5, 1, 612.5, 0, -15);
        step(30, "wait", () -> { });
        expect(Stage.FREE);
        shot("25_salavat_good_ending");
        tp(46.5, 1, 650.5, -120, -10);
        shot("26_salavat_from_cliff");
        tp(598.5, 1, 590.5, 0, -25);
        shot("27_friendship_monument");
        tp(384.5, 1, 470.5, 0, 0);
        shot("28_lenin_park");
        tp(360.5, 1, 590.5, 0, -15);
        shot("29_house_of_republic");
        tp(250.5, 1, 446.5, 180, -15);
        shot("30_bsmu_building_2");
        tp(480.5, 1, 345.5, -40, -10);
        shot("31_sovetskaya_square");
        tp(418.5, 1, 150.5, 0, 0);
        shot("32_art_kvadrat");
        tp(496.5, 1, 584.5, 0, -20);
        shot("33_mosque");
        tp(160.5, 1, 448.5, 150, -15);
        shot("34_congress_hall_tv_tower");
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
            script();
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
