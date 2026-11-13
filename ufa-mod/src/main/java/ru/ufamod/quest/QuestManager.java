package ru.ufamod.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import ru.ufamod.UfaMod;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.net.Net;
import ru.ufamod.net.OpenExamPacket;
import ru.ufamod.registry.ModItems;
import ru.ufamod.world.Places;
import ru.ufamod.world.Plan;
import ru.ufamod.world.UfaWorld;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** The story: one day of a dentistry student of the Bashkir State Medical University. */
@Mod.EventBusSubscriber(modid = UfaMod.MODID)
public final class QuestManager {
    private QuestManager() {}

    private static final String TAG = "ufamod";
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final Map<UUID, int[]> TICKETS = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static final int RANGE_HITS = 5, TRENCH_BLOCKS = 12;

    // ------------------------------------------------------------------ player state

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(TAG)) persisted.put(TAG, new CompoundTag());
        return persisted.getCompound(TAG);
    }

    public static Stage stage(Player player) {
        return Stage.byName(data(player).getString("stage"));
    }

    public static void setStage(ServerPlayer player, Stage stage) {
        CompoundTag d = data(player);
        d.putString("stage", stage.name());
        d.putInt("timer", 0);
        d.putInt("count", 0);
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.0f);
        updateBar(player);
    }

    // ------------------------------------------------------------------ messages

    private static void say(ServerPlayer player, String who, String text) {
        player.sendSystemMessage(Component.literal(who + ": ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE)));
    }

    private static void narrate(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static void title(ServerPlayer player, String title, String subtitle, ChatFormatting color) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(color, ChatFormatting.BOLD)));
    }

    private static MutableComponent button(String label, String command, ChatFormatting color) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(color).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    // ------------------------------------------------------------------ objectives

    private record Objective(String text, @Nullable BlockPos target) {}

    private static double dist(ServerPlayer player, BlockPos p) {
        return Math.sqrt(player.distanceToSqr(p.getX() + 0.5, player.getY(), p.getZ() + 0.5));
    }

    private static Objective objective(ServerPlayer player) {
        int count = data(player).getInt("count");
        Places P = Places.get();
        return switch (stage(player)) {
            case WAKE -> new Objective("07:00 · Возьми из шкафа (сундук в комнате) халат и зачётку", P.wardrobe);
            case BREAKFAST -> new Objective("07:15 · Позавтракай: еда в холодильнике — втором сундуке у двери", P.fridge);
            case TO_BSMU -> dist(player, P.bsmuDoor) > 500
                    ? new Objective("07:40 · На пары: сядь в маршрутку у входа в общежитие", P.busFrom)
                    : new Objective("08:10 · Главный корпус БГМУ, ул. Ленина, 3", P.bsmuDoor);
            case WATCHMAN -> new Objective("08:20 · Надень халат и покажись охраннику на вахте", P.guard);
            case LECTURE -> new Objective("08:30 · Лекция по биохимии: большая аудитория в центре корпуса", P.lecture);
            case LECTURE_RUNNING -> new Objective("08:30 · Слушай лекцию…", null);
            case TO_EXAM -> new Objective("11:00 · Экзамен: кафедра анатомии, корпус №2 (Заки Валиди, 47). Атлас — в Нацбиблиотеке напротив", P.exam);
            case EXAM_RUNNING -> new Objective("11:00 · Отвечай на билет", null);
            case PASSED -> dist(player, P.salavat) > 500
                    ? new Objective("Сдал! К памятнику Салавату Юлаеву: маршрутка у главного корпуса", P.busTo)
                    : new Objective("Сдал! Отпразднуй у памятника Салавату Юлаеву", P.salavat);
            case FAILED -> new Objective("Неуд… Выйди на улицу", P.examOut);
            case ARREST -> new Objective("…", null);
            case CAMP_REPORT -> new Objective("Учебный центр · Доложи сержанту на плацу", Plan.CAMP_SERGEANT);
            case CAMP_COURSE -> new Objective("Пробеги полосу препятствий до финиша", Plan.CAMP_COURSE_END);
            case CAMP_RANGE -> new Objective("Стрельбище: попади в мишени из лука (" + count + "/" + RANGE_HITS + ")", Plan.CAMP_RANGE);
            case CAMP_TRENCH -> new Objective("Окопы: выкопай землю лопаткой (" + count + "/" + TRENCH_BLOCKS + ")", Plan.CAMP_TRENCH);
            case CAMP_TRUCK -> new Objective("Погрузка: поднимись в кузов «Урала»", Plan.CAMP_TRUCK);
            default -> null;
        };
    }

    private static void updateBar(ServerPlayer player) {
        Objective o = objective(player);
        ServerBossEvent bar = BARS.get(player.getUUID());
        if (o == null) {
            if (bar != null) {
                bar.removeAllPlayers();
                BARS.remove(player.getUUID());
            }
            return;
        }
        Stage stage = stage(player);
        if (bar == null) {
            bar = new ServerBossEvent(Component.literal(o.text()), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            BARS.put(player.getUUID(), bar);
        }
        bar.setName(Component.literal(o.text()));
        bar.setColor(stage.inCamp() || stage == Stage.FAILED || stage == Stage.ARREST ? BossEvent.BossBarColor.RED
                : stage == Stage.PASSED ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.YELLOW);
        float progress = 1f;
        if (stage == Stage.CAMP_RANGE) progress = data(player).getInt("count") / (float) RANGE_HITS;
        if (stage == Stage.CAMP_TRENCH) progress = data(player).getInt("count") / (float) TRENCH_BLOCKS;
        bar.setProgress(Mth.clamp(progress, 0f, 1f));
        if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
    }

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static void showCompass(ServerPlayer player) {
        Objective o = objective(player);
        if (o == null || o.target() == null) return;
        BlockPos t = o.target();
        double dx = t.getX() + 0.5 - player.getX(), dz = t.getZ() + 0.5 - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        int dy = t.getY() - player.getBlockY();
        String height = dy >= 3 ? " · выше" : dy <= -3 ? " · ниже" : "";
        String text;
        if (dist < 4 && height.isEmpty()) text = "● ты на месте";
        else {
            float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
            int idx = Math.floorMod(Math.round(Mth.wrapDegrees(yaw - player.getYRot()) / 45f), 8);
            text = ARROWS[idx] + "  " + (int) dist + " м" + height;
        }
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GOLD), true);
    }

    // ------------------------------------------------------------------ starting, restarting, leaving

    public static void start(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel ufa = UfaWorld.level(server);
        if (ufa == null) {
            player.sendSystemMessage(Component.literal("Измерение «Уфа» не найдено — создайте новый мир с установленным модом.").withStyle(ChatFormatting.RED));
            return;
        }
        TICKETS.remove(player.getUUID());
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(12);
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.setGameMode(GameType.ADVENTURE);
        server.overworld().setDayTime(1000);
        server.overworld().setWeatherParameters(24000 * 3, 0, false, false);
        BlockPos wake = Places.get().wake;
        player.teleportTo(ufa, wake.getX() + 0.5, wake.getY(), wake.getZ() + 0.5, 0f, 0f);
        player.setRespawnPosition(UfaWorld.UFA, wake, 0f, true, false);
        CompoundTag d = data(player);
        d.remove("grade");
        d.remove("lecture");
        setStage(player, Stage.WAKE);
        title(player, "Уфа", "День экзамена", ChatFormatting.GOLD);
        narrate(player, "07:00. Общежитие БГМУ на улице Мингажева. Будильник звонит уже третий раз.");
        narrate(player, "Ты — студент стоматологического факультета. Сегодня лекция по биохимии и экзамен по анатомии.");
        say(player, "Сосед Азамат", "Вставай, проспишь! Халат и зачётка в шкафу — сундук в углу. И атлас возьми, повторишь по дороге.");
    }

    public static void free(ServerPlayer player) {
        setStage(player, Stage.FREE);
        player.setGameMode(GameType.CREATIVE);
        player.removeAllEffects();
        if (player.level().dimension() != UfaWorld.UFA || player.getX() > 3500) teleport(player, Places.get().bsmuDoor);
        narrate(player, "Свободная прогулка по Уфе. Режим — творческий. Вернуться к сюжету: /ufa restart");
        narrate(player, "Быстрые перемещения: /ufa tp bsmu | dorm | anatomy | library | gostiny | opera | park | salavat | friendship | camp");
    }

    public static void leave(ServerPlayer player) {
        setStage(player, Stage.FREE);
        ServerLevel overworld = player.server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        player.setRespawnPosition(net.minecraft.world.level.Level.OVERWORLD, null, 0f, false, false);
        player.teleportTo(overworld, spawn.getX() + 0.5, overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()) + 1, spawn.getZ() + 0.5, 0f, 0f);
        narrate(player, "Ты вернулся в обычный мир. Обратно в Уфу: /ufa restart или /ufa free");
    }

    public static void teleport(ServerPlayer player, BlockPos pos) {
        ServerLevel ufa = UfaWorld.level(player.server);
        if (ufa != null) player.teleportTo(ufa, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0f);
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (stage(player) == Stage.NONE) start(player);
        else updateBar(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerBossEvent bar = BARS.remove(event.getEntity().getUUID());
        if (bar != null) bar.removeAllPlayers();
        TICKETS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) updateBar(player);
    }

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (stage(player) == Stage.BREAKFAST && event.getItem().isEdible()) {
            setStage(player, Stage.TO_BSMU);
            narrate(player, "Теперь на пары. До главного корпуса на Ленина, 3 почти два километра через центр.");
            narrate(player, "У входа в общежитие стоит маршрутка — поговори с водителем. Или иди пешком, если не боишься опоздать.");
        }
    }

    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer player)) return;
        if (stage(player) != Stage.CAMP_RANGE || !(event.getRayTraceResult() instanceof BlockHitResult hit)) return;
        if (!player.level().getBlockState(hit.getBlockPos()).is(Blocks.TARGET)) return;
        CompoundTag d = data(player);
        int count = d.getInt("count") + 1;
        d.putInt("count", count);
        player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.7f, 1.0f);
        if (count >= RANGE_HITS) {
            setStage(player, Stage.CAMP_TRENCH);
            player.getInventory().add(Loot.sapperShovel());
            say(player, "Сержант Громов", "Сойдёт. Теперь инженерная подготовка. Лопатку в руки — и копать окоп. Двенадцать блоков!");
        } else updateBar(player);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || stage(player) != Stage.CAMP_TRENCH) return;
        if (!Plan.CAMP_TRENCH_BOX.contains(Vec3.atCenterOf(event.getPos()))) return;
        CompoundTag d = data(player);
        int count = d.getInt("count") + 1;
        d.putInt("count", count);
        if (count >= TRENCH_BLOCKS) {
            setStage(player, Stage.CAMP_TRUCK);
            say(player, "Сержант Громов", "Курс молодого бойца окончен. Учебка закончилась. «Урал» у ворот — грузись в кузов.");
        } else updateBar(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Stage stage = stage(player);
        if (stage == Stage.NONE || stage == Stage.FREE) return;
        CompoundTag d = data(player);
        int timer = d.getInt("timer") + 1;
        d.putInt("timer", timer);
        if (timer % 10 == 0) {
            if (!BARS.containsKey(player.getUUID())) updateBar(player);
            showCompass(player);
        }
        switch (stage) {
            case WAKE -> {
                if (timer % 10 == 0 && has(player, ModItems.LAB_COAT.get()) && has(player, ModItems.RECORD_BOOK.get())) {
                    setStage(player, Stage.BREAKFAST);
                    narrate(player, "На голодный желудок анатомию не сдают. Холодильник — второй сундук, у двери.");
                }
            }
            case TO_BSMU -> {
                if (timer % 20 == 0) updateBar(player);
                Places P = Places.get();
                if (timer % 10 == 0 && (dist(player, P.bsmuDoor) < 14 || P.insideBuilding(player.getX(), player.getZ(), P.bsmuIndex))) {
                    setStage(player, Stage.WATCHMAN);
                    narrate(player, "Главный корпус БГМУ. Построен в 1930-е, во время войны здесь работал эвакуированный 1-й Московский мединститут.");
                    narrate(player, "На вахте пропускной режим: без халата не пустят.");
                }
            }
            case LECTURE_RUNNING -> lecture(player, timer);
            case PASSED -> {
                if (timer % 20 == 0) updateBar(player);
                if (timer % 10 == 0 && dist(player, Places.get().salavat) < 26) goodEnding(player);
            }
            case FAILED -> {
                Places P = Places.get();
                if (timer % 10 == 0 && P.outdoors(player.getX(), player.getZ()) && dist(player, P.examOut) < 60) beginArrest(player);
            }
            case ARREST -> arrest(player, timer);
            case CAMP_COURSE -> {
                if (timer % 5 == 0 && Plan.CAMP_COURSE_FINISH.contains(player.position())) {
                    setStage(player, Stage.CAMP_RANGE);
                    player.getInventory().add(new ItemStack(Items.BOW));
                    player.getInventory().add(new ItemStack(Items.ARROW, 48));
                    say(player, "Сержант Громов", "Живой? Тогда на стрельбище. Автоматов на всех не хватило — держи лук. Пять попаданий в мишени!");
                }
            }
            case CAMP_TRUCK -> {
                if (timer % 5 == 0 && Plan.CAMP_TRUCK_BOX.contains(player.position())) badEnding(player);
            }
            case END_SVO -> {
                if (timer % 60 == 0) player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
            }
            default -> { }
        }
    }

    private static boolean has(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().hasAnyMatching(stack -> stack.is(item));
    }

    private static boolean wearsCoat(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.LAB_COAT.get());
    }

    // ------------------------------------------------------------------ talking to NPCs

    private static final String[] CITIZEN_LINES = {
            "Здравствуй, сынок. На учёбу? Ну, с Богом.",
            "Айда вечером на Салавата — закат над Белой смотреть!",
            "В Гостином дворе сегодня ярмарка: мёд бурзянский, липовый. Возьми баночку маме.",
            "Опять автобуса нет… Пешком до Телецентра быстрее дойду.",
            "Эчпочмак горячий будешь? В пекарне на углу только из печи.",
            "Ты из меда? Зубы мне потом полечишь, студент!",
            "Раньше Заки Валиди улицей Фрунзе называлась. А ещё раньше — Большой Ильинской.",
            "Монумент Дружбы видел? Тридцать метров гранита. А стоит на том месте, где Уфимский кремль был.",
            "В оперном сегодня «Щелкунчик». Там Нуреев когда-то впервые на сцену вышел, между прочим.",
            "Хорошая у нас Уфа. Зелёная. Только горки — пока дойдёшь, упаришься."
    };

    private static final String[] STUDENT_LINES = {
            "Ты выучил черепные нервы? Их двенадцать пар, двенадцать!",
            "Говорят, Латыпов сегодня злой. Половину потока уже отправил на пересдачу.",
            "Mandibula — нижняя, maxilla — верхняя. Mandibula — нижняя, maxilla — верхняя…",
            "Если завалю — отчислят. А там сам знаешь: отсрочка сгорает, и здравствуй, военкомат.",
            "В Национальной библиотеке напротив главного корпуса атлас лежит на пюпитре. Сходи, повтори.",
            "После экзамена — в столовую за эчпочмаком. Если выживем."
    };

    public static void onTalk(ServerPlayer player, NpcEntity npc) {
        Stage stage = stage(player);
        String name = npc.hasCustomName() ? npc.getCustomName().getString() : "Прохожий";
        switch (npc.getRole()) {
            case ROOMMATE -> say(player, name, stage == Stage.WAKE ? "Шкаф у окна. Халат, зачётка, атлас — и бегом."
                    : stage == Stage.BREAKFAST ? "Поешь нормально. В холодильнике у двери эчпочмаки, мама из Сибая передала."
                    : "Ни пуха ни пера! …Ну? Говори «к чёрту»!");
            case WATCHMAN -> {
                if (npc.getVariant() == 1) {
                    say(player, name, stage.ordinal() <= Stage.TO_BSMU.ordinal() ? "На экзамен? Иди, иди. И чтоб до одиннадцати вечера был дома!"
                            : "Гостей не водить, плитки в комнатах не включать.");
                } else if (stage == Stage.WATCHMAN) {
                    if (wearsCoat(player)) {
                        setStage(player, Stage.LECTURE);
                        say(player, name, "Халат вижу, проходи. Лекция в большой аудитории, прямо по коридору, в середине корпуса.");
                    } else if (has(player, ModItems.LAB_COAT.get())) {
                        say(player, name, "А халат почему в сумке? Надевай — тогда пущу. Порядок есть порядок.");
                    } else {
                        say(player, name, "Без халата не пущу! Возвращайся в общежитие, он у тебя в шкафу остался.");
                    }
                } else say(player, name, "Проходи, не задерживайся. Сменку не забывай.");
            }
            case COOK -> {
                if (!has(player, ModItems.ECHPOCHMAK.get()) && stage != Stage.NONE && !stage.inCamp()) {
                    player.getInventory().add(new ItemStack(ModItems.ECHPOCHMAK.get()));
                    say(player, name, "Бери эчпочмак, улым, горячий. Студент должен быть сытым.");
                } else say(player, name, "Кушай, кушай. Ещё чак-чак на раздаче возьми.");
            }
            case LECTURER -> {
                if (stage == Stage.LECTURE) {
                    setStage(player, Stage.LECTURE_RUNNING);
                    say(player, name, "Опаздываем? Садитесь. Тема лекции — «Гликолиз».");
                } else if (stage == Stage.LECTURE_RUNNING) say(player, name, "Тише. Записывайте.");
                else say(player, name, "Биохимию учить надо было в семестре, а не в ночь перед экзаменом.");
            }
            case LIBRARIAN -> {
                if (!has(player, Items.WRITTEN_BOOK) && !stage.inCamp()) {
                    player.getInventory().add(Loot.atlas());
                    say(player, name, "Атлас анатомии? Держите. Вернуть до конца сессии. И тише, пожалуйста.");
                } else say(player, name, "Тс-с! Читальный зал. Атлас на пюпитре рядом — там всё, что спросят.");
            }
            case PROFESSOR -> {
                if (stage == Stage.TO_EXAM || stage == Stage.EXAM_RUNNING) {
                    if (stage == Stage.TO_EXAM) {
                        say(player, name, "Зачётку на стол. Тяните билет. Пять вопросов — для «удовлетворительно» нужно три верных ответа.");
                        setStage(player, Stage.EXAM_RUNNING);
                    }
                    openExam(player);
                } else if (stage.ordinal() < Stage.TO_EXAM.ordinal()) say(player, name, "Экзамен после лекции. Не мешайте готовить билеты.");
                else if (stage == Stage.PASSED) say(player, name, "Вы свободны, коллега. Не забывайте: эмаль не восстанавливается — как и репутация.");
                else say(player, name, "Мне больше нечего вам сказать. Следующий!");
            }
            case STUDENT -> say(player, name, STUDENT_LINES[RANDOM.nextInt(STUDENT_LINES.length)]);
            case COMMISSAR -> say(player, name, "Не задерживаемся, гражданин.");
            case SOLDIER -> say(player, name, stage.inCamp() ? "Не положено. Отойди от ворот." : "…");
            case SERGEANT -> {
                if (stage == Stage.CAMP_REPORT) {
                    setStage(player, Stage.CAMP_COURSE);
                    say(player, name, "Студент? Стоматолог? Ничего, здесь тебя быстро научат. Марш на полосу препятствий — добежать до финиша!");
                } else if (stage == Stage.CAMP_COURSE) say(player, name, "Полоса препятствий — за казармами. Бегом!");
                else if (stage == Stage.CAMP_RANGE) say(player, name, "Стрельбище рядом с полосой. Пять попаданий.");
                else if (stage == Stage.CAMP_TRENCH) say(player, name, "Копай. Окоп солдата бережёт.");
                else if (stage == Stage.CAMP_TRUCK) say(player, name, "В кузов. Не заставляй ждать.");
                else say(player, name, "Вольно.");
            }
            case RECRUIT -> say(player, name, RANDOM.nextBoolean() ? "Тоже с пересдачи? Я вот сопромат не сдал…" : "Эх, надо было учить.");
            case DRIVER -> ride(player, npc, stage, name);
            default -> say(player, name, CITIZEN_LINES[RANDOM.nextInt(CITIZEN_LINES.length)]);
        }
    }

    /** The minibus: between the dormitory and the university, and to the Salavat monument after the exam. */
    private static void ride(ServerPlayer player, NpcEntity npc, Stage stage, String name) {
        if (stage.inCamp() || stage == Stage.ARREST) return;
        Places P = Places.get();
        boolean atDorm = npc.distanceToSqr(P.busFrom.getX() + 0.5, npc.getY(), P.busFrom.getZ() + 0.5) < 40 * 40;
        BlockPos dest;
        if (atDorm) {
            dest = P.bsmuDoor;
            say(player, name, "До Медуниверситета? Садись. Передаём за проезд!");
            narrate(player, "Маршрутка летит по Мингажева и Коммунистической, сворачивает на Ленина. Десять минут — и ты у главного корпуса.");
        } else if (stage == Stage.PASSED) {
            dest = P.salavat;
            say(player, name, "К Салавату? Сдал, значит! Поехали, довезу до самой площадки.");
            narrate(player, "По Заки Валиди, мимо Телецентра — к обрыву над Белой.");
        } else {
            dest = P.dormOut;
            say(player, name, "До общежития на Мингажева? Поехали.");
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
        teleport(player, dest);
        player.playNotifySound(SoundEvents.MINECART_RIDING, SoundSource.AMBIENT, 0.7f, 1.2f);
    }

    // ------------------------------------------------------------------ lecture

    private static final String[] LECTURE = {
            "Гликолиз — это расщепление глюкозы до пирувата. Десять реакций, все — в цитоплазме клетки.",
            "Из одной молекулы глюкозы получаем две молекулы пирувата, два АТФ и два НАДН. Запишите.",
            "Ключевой фермент — фосфофруктокиназа. Его тормозит избыток АТФ: клетка не тратит глюкозу зря.",
            "Без кислорода пируват превращается в лактат. Бактерии зубного налёта делают то же самое —",
            "и получившаяся кислота растворяет эмаль. Так биохимия объясняет кариес. Стоматологам — знать!",
            "На сегодня всё. У кого экзамен по анатомии — кафедра во втором корпусе, на Заки Валиди, 47. Удачи."
    };

    private static void lecture(ServerPlayer player, int timer) {
        if (timer % 70 != 0) return;
        int line = timer / 70 - 1;
        if (line < LECTURE.length) say(player, "Доцент Хабибуллина", LECTURE[line]);
        else {
            data(player).putBoolean("lecture", true);
            setStage(player, Stage.TO_EXAM);
            narrate(player, "Кафедра анатомии — во флигеле бывшей мужской гимназии (корпус №2), сразу за главным корпусом.");
            narrate(player, "До экзамена есть время: в Национальной библиотеке через улицу лежит атлас, в столовой БГМУ во дворе кормят.");
        }
    }

    // ------------------------------------------------------------------ exam

    private static void openExam(ServerPlayer player) {
        int[] ticket = ExamQuestions.ticket(RANDOM);
        TICKETS.put(player.getUUID(), ticket);
        Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenExamPacket(ticket));
    }

    public static void onExamSubmitted(ServerPlayer player, byte[] answers) {
        int[] ticket = TICKETS.remove(player.getUUID());
        if (ticket == null || stage(player) != Stage.EXAM_RUNNING) return;
        int score = ExamQuestions.score(ticket, answers);
        String grade = score == 5 ? "отлично" : score == 4 ? "хорошо" : score == 3 ? "удовлетворительно" : "неудовлетворительно";
        data(player).putString("grade", grade);
        narrate(player, "Верных ответов: " + score + " из " + ExamQuestions.TICKET_SIZE + ".");
        if (score >= ExamQuestions.PASS_SCORE) {
            setStage(player, Stage.PASSED);
            title(player, "Сдал!", "Анатомия: «" + grade + "»", ChatFormatting.GREEN);
            say(player, "Профессор Латыпов", score == 5 ? "Блестяще. Давайте зачётку. Из вас выйдет хороший врач."
                    : "Что ж… Ставлю «" + grade + "». Давайте зачётку и идите с миром.");
            narrate(player, "Гора с плеч! Уфимцы отмечают важные события у памятника Салавату Юлаеву над Белой — айда туда.");
            fireworks(player);
        } else {
            setStage(player, Stage.FAILED);
            title(player, "Неуд.", "Анатомия не сдана", ChatFormatting.RED);
            say(player, "Профессор Латыпов", "Это не знания, это стоматологическая катастрофа. «Неудовлетворительно».");
            say(player, "Профессор Латыпов", "Пересдач у вас не осталось. Документы на отчисление уйдут в деканат сегодня же.");
            narrate(player, "Отчислен. А значит, отсрочки от армии больше нет…");
        }
    }

    public static void showRecordBook(ServerPlayer player) {
        CompoundTag d = data(player);
        player.sendSystemMessage(Component.literal("— Зачётная книжка —").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("БГМУ, стоматологический факультет, 2 курс"));
        player.sendSystemMessage(Component.literal("Биохимия (лекция): " + (d.getBoolean("lecture") ? "посещена" : "—")));
        player.sendSystemMessage(Component.literal("Анатомия (экзамен): " + (d.contains("grade") ? d.getString("grade") : "—")));
    }

    private static void fireworks(ServerPlayer player) {
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 1);
        explosion.putIntArray("Colors", new int[]{0x2E86DE, 0xFFFFFF, 0x27AE60});
        explosion.putBoolean("Flicker", true);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        list.add(explosion);
        CompoundTag fw = new CompoundTag();
        fw.put("Explosions", list);
        fw.putByte("Flight", (byte) 1);
        rocket.getOrCreateTag().put("Fireworks", fw);
        for (int i = 0; i < 3; i++)
            player.level().addFreshEntity(new FireworkRocketEntity(player.level(), player.getX() + (i - 1) * 3, player.getY() + 7, player.getZ() + 3, rocket));
    }

    private static void goodEnding(ServerPlayer player) {
        setStage(player, Stage.FREE);
        title(player, "Хороший финал", "Сессия продолжается…", ChatFormatting.GREEN);
        fireworks(player);
        narrate(player, "Салават Юлаев смотрит на Белую, а ты — на закрытый экзамен. Впереди ещё гистология, но это уже завтра.");
        player.sendSystemMessage(Component.literal("Что дальше? ").append(button("Гулять по Уфе", "/ufa free", ChatFormatting.GREEN))
                .append(" ").append(button("Прожить день заново", "/ufa restart", ChatFormatting.YELLOW)));
    }

    // ------------------------------------------------------------------ the arrest and the camp

    private static void beginArrest(ServerPlayer player) {
        setStage(player, Stage.ARREST);
        ServerLevel level = player.serverLevel();
        // the patrol waits a few steps away from the door, on the far side from the building
        Places P = Places.get();
        double dx = P.examOut.getX() - P.exam.getX(), dz = P.examOut.getZ() - P.exam.getZ();
        double len = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
        dx /= len;
        dz /= len;
        String[] names = {"Военком", "Военный патруль", "Военный патруль"};
        NpcRole[] roles = {NpcRole.COMMISSAR, NpcRole.SOLDIER, NpcRole.SOLDIER};
        double[][] offsets = {{9, 0}, {11, -3}, {11, 3}};
        for (int i = 0; i < 3; i++) {
            double x = player.getX() + dx * offsets[i][0] - dz * offsets[i][1], z = player.getZ() + dz * offsets[i][0] + dx * offsets[i][1];
            for (int k = 0; k < 8 && !P.outdoors(x, z); k++) {
                x -= dx;
                z -= dz;
            }
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            NpcEntity npc = UfaWorld.spawnNpc(level, x, y, z, 0f, roles[i], names[i], 0, false);
            if (npc == null) continue;
            npc.setLifetime(20 * 20);
            npc.follow(player);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 20, 5, false, false));
        narrate(player, "У крыльца кафедры тебя уже ждут люди в форме.");
    }

    private static void arrest(ServerPlayer player, int timer) {
        switch (timer) {
            case 40 -> say(player, "Военком", "Гражданин студент! Одну минуту. Вернее — уже не студент, верно?");
            case 110 -> say(player, "Военком", "Приказ об отчислении подписан. Отсрочка аннулирована.");
            case 180 -> {
                say(player, "Военком", "Вот повестка. Распишитесь. Пройдёмте в автобус.");
                player.getInventory().add(new ItemStack(ModItems.SUMMONS.get()));
            }
            case 240 -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
                title(player, "Мобилизован", "Несколько дней спустя…", ChatFormatting.RED);
            }
            case 300 -> arriveAtCamp(player);
            default -> { }
        }
    }

    private static void arriveAtCamp(ServerPlayer player) {
        ServerLevel ufa = UfaWorld.level(player.server);
        if (ufa == null) return;
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CAMO_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CAMO_JACKET.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CAMO_TROUSERS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.ARMY_BOOTS.get()));
        player.getInventory().add(new ItemStack(ModItems.SUMMONS.get()));
        player.getInventory().add(new ItemStack(Items.BREAD, 6));
        Vec3 p = Plan.CAMP_ARRIVE;
        player.teleportTo(ufa, p.x, p.y, p.z, -90f, 0f);
        player.setRespawnPosition(UfaWorld.UFA, BlockPos.containing(p), -90f, true, false);
        player.server.overworld().setDayTime(23500);
        setStage(player, Stage.CAMP_REPORT);
        narrate(player, "Учебный центр. Подъём в шесть, халат сменили на камуфляж.");
        say(player, "Сержант Громов", "Новенький! Ко мне, на плац!");
    }

    private static void badEnding(ServerPlayer player) {
        setStage(player, Stage.END_SVO);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100000, 10, false, false));
        player.playNotifySound(SoundEvents.MINECART_RIDING, SoundSource.AMBIENT, 1f, 0.6f);
        title(player, "Тебя отправили на СВО", "Конец. Надо было учить анатомию.", ChatFormatting.DARK_RED);
        narrate(player, "Брезент опускается. «Урал» трогается и выезжает за ворота.");
        narrate(player, "В кармане — повестка и зачётка с единственной записью: «анатомия — неуд.»");
        player.sendSystemMessage(Component.literal("Плохой финал. ").withStyle(ChatFormatting.RED)
                .append(button("Прожить день заново", "/ufa restart", ChatFormatting.YELLOW))
                .append(" ").append(button("Гулять по Уфе", "/ufa free", ChatFormatting.GREEN)));
    }

    // ------------------------------------------------------------------ dev / command helpers

    public static void forceStage(ServerPlayer player, Stage stage) {
        if (stage == Stage.CAMP_REPORT) {
            arriveAtCamp(player);
            return;
        }
        if (stage == Stage.CAMP_RANGE) {
            player.getInventory().add(new ItemStack(Items.BOW));
            player.getInventory().add(new ItemStack(Items.ARROW, 48));
        }
        if (stage == Stage.CAMP_TRENCH) player.getInventory().add(Loot.sapperShovel());
        setStage(player, stage);
    }
}
