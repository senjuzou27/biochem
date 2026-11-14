package ru.ufamod.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import ru.ufamod.entity.NpcEntity;
import ru.ufamod.entity.NpcRole;
import ru.ufamod.registry.ModItems;
import ru.ufamod.world.Military;
import ru.ufamod.world.Plan;
import ru.ufamod.world.UfaWorld;

import java.util.List;
import java.util.Random;

/**
 * The front line of the bad ending: a night in a trench at an invented place, three waves of an enemy
 * without a name, a wounded comrade to carry back, and the shell that sends the student home.
 */
public final class Front {
    private Front() {}

    public static final int WAVES = 3;
    private static final int[] WAVE_SIZE = {4, 5, 6};
    private static final Random RANDOM = new Random();
    private static final String COMMANDER = "Командир взвода «Сокол»", MEDIC = "Санинструктор Лена", COMRADE = "Мобилизованный Ильдар";

    // ------------------------------------------------------------------ arrival

    public static void start(ServerPlayer player) {
        ServerLevel ufa = UfaWorld.level(player.server);
        if (ufa == null) return;
        QuestManager.setStage(player, Stage.FRONT_ARRIVE);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
        QuestManager.title(player, "Несколько недель спустя", "Где-то далеко от Уфы", ChatFormatting.GRAY);
        QuestManager.narrate(player, "Брезент опускается. «Урал» трогается и выезжает за ворота учебного центра.");
        QuestManager.narrate(player, "Дорога длиной в несколько суток. Высадили ночью, у разбитого хутора. Названия у этого места нет — только номер на карте.");
        Vec3 p = Plan.FRONT_ARRIVE;
        player.teleportTo(ufa, p.x, p.y, p.z, -90f, 0f);
        player.setRespawnPosition(UfaWorld.UFA, BlockPos.containing(p), -90f, true, false);
        player.server.overworld().setDayTime(13800);
        player.server.overworld().setWeatherParameters(0, 24000, true, false);
        giveKit(player);
        QuestManager.say(player, COMMANDER, "Пополнение? Студент, говоришь… Ко мне, получишь задачу.");
    }

    /** Rifle and rounds, field dressings; again after a death if they were lost. */
    static void giveKit(ServerPlayer player) {
        boolean armed = false;
        for (ItemStack it : player.getInventory().items) {
            String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it.getItem()).toString();
            if (it.is(Items.BOW) || id.equals("tacz:modern_kinetic_gun")) armed = true;
        }
        if (!armed) {
            ItemStack rifle = Military.rifle(), ammo = Military.ammo(150);
            if (rifle != null && ammo != null) {
                player.getInventory().add(rifle);
                player.getInventory().add(ammo);
            } else {
                player.getInventory().add(new ItemStack(Items.BOW));
                player.getInventory().add(new ItemStack(Items.ARROW, 64));
            }
        }
        ItemStack kit = Military.warfareItem("medical_kit", 2);
        if (kit != null) player.getInventory().add(kit);
        else player.getInventory().add(PotionUtils.setPotion(new ItemStack(Items.POTION, 2), Potions.HEALING));
        player.getInventory().add(new ItemStack(Items.BREAD, 4));
    }

    public static void onRespawn(ServerPlayer player) {
        QuestManager.narrate(player, "Тебя оттащили в блиндаж. Санинструктор перевязала и отправила обратно.");
        giveKit(player);
        if (QuestManager.data(player).getBoolean("carry")) QuestManager.data(player).putBoolean("carry", false);
    }

    // ------------------------------------------------------------------ objectives

    static String objective(ServerPlayer player, Stage stage) {
        CompoundTag d = QuestManager.data(player);
        return switch (stage) {
            case FRONT_ARRIVE -> "Блиндаж · Доложи командиру взвода";
            case FRONT_HOLD -> d.getInt("wave") == 0 ? "Займи позицию в окопе, на стрелковой ступени"
                    : "Удержи позицию · атака " + d.getInt("wave") + "/" + WAVES + " · противников: " + d.getInt("left") + " · пригнись (Shift), чтобы укрыться";
            case FRONT_MEDEVAC -> d.getBoolean("carry") ? "Неси Ильдара в блиндаж, к санинструктору" : "Ильдар ранен на наблюдательном посту за проволокой — вытащи его";
            case FRONT_WOUNDED -> "…";
            default -> null;
        };
    }

    static BlockPos target(ServerPlayer player, Stage stage) {
        return switch (stage) {
            case FRONT_ARRIVE -> Plan.FRONT_COMMANDER;
            case FRONT_HOLD -> Plan.FRONT_TRENCH;
            case FRONT_MEDEVAC -> QuestManager.data(player).getBoolean("carry") ? Plan.FRONT_MEDIC : Plan.FRONT_OP;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ talking

    /** Returns true when the NPC belongs to the front. */
    static boolean onTalk(ServerPlayer player, NpcEntity npc, Stage stage, String name) {
        switch (npc.getRole()) {
            case COMMANDER -> {
                if (stage == Stage.FRONT_ARRIVE) {
                    QuestManager.setStage(player, Stage.FRONT_HOLD);
                    QuestManager.say(player, name, "Задача простая: держать этот окоп. Выходи по ходу сообщения, вставай на ступень у бруствера.");
                    QuestManager.say(player, name, "Стреляй через бойницы между мешками. Голову не высовывай. Услышишь свист — пригнись.");
                } else if (stage == Stage.FRONT_HOLD) QuestManager.say(player, name, "В окоп! Держать позицию!");
                else if (stage == Stage.FRONT_MEDEVAC) QuestManager.say(player, name, "Ильдар на посту за проволокой, проход слева. Тащи его сюда, к Лене.");
                else QuestManager.say(player, name, "Отдыхай, пока тихо.");
                return true;
            }
            case MEDIC -> {
                if (stage == Stage.FRONT_MEDEVAC && QuestManager.data(player).getBoolean("carry")) deliver(player);
                else if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(8f);
                    QuestManager.say(player, name, "Стой ровно. Вот так. Повязку не трогай.");
                } else QuestManager.say(player, name, "Ты же медик почти? Значит, понимаешь, как мало тут можно сделать.");
                return true;
            }
            case WOUNDED -> {
                if (stage == Stage.FRONT_MEDEVAC && !QuestManager.data(player).getBoolean("carry")) {
                    QuestManager.data(player).putBoolean("carry", true);
                    npc.discard();
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 600, 1, false, false));
                    QuestManager.say(player, COMRADE, "Студент… Ты? Нога… Не чувствую ногу.");
                    QuestManager.narrate(player, "Жгут выше раны — бедренная артерия, ты помнишь это из атласа. Взваливаешь Ильдара на плечи.");
                    QuestManager.updateBar(player);
                } else QuestManager.say(player, COMRADE, "…");
                return true;
            }
            case ENEMY -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------ every second of the fight

    static void tick(ServerPlayer player, Stage stage, int timer) {
        ServerLevel level = player.serverLevel();
        if (!Plan.FRONT_BOX.contains(player.position())) return;
        switch (stage) {
            case FRONT_HOLD -> hold(player, level, timer);
            case FRONT_MEDEVAC -> {
                if (timer % 140 == 0) shell(player, level, 14);
                if (timer % 10 == 0 && QuestManager.data(player).getBoolean("carry")
                        && player.distanceToSqr(Vec3.atCenterOf(Plan.FRONT_MEDIC)) < 16) deliver(player);
            }
            case FRONT_WOUNDED -> wounded(player, level, timer);
            default -> { }
        }
    }

    private static List<NpcEntity> enemies(ServerLevel level) {
        return level.getEntitiesOfClass(NpcEntity.class, Plan.FRONT_BOX, e -> e.isAlive() && e.getRole() == NpcRole.ENEMY);
    }

    private static void hold(ServerPlayer player, ServerLevel level, int timer) {
        CompoundTag d = QuestManager.data(player);
        int wave = d.getInt("wave");
        if (wave == 0) {
            // the attack begins once the player has taken up the position
            boolean inTrench = Math.abs(player.getX() - Plan.FRONT_TRENCH.getX()) < 6 && player.getY() < Plan.BASE_Y + 2;
            if (timer % 10 == 0 && (inTrench || timer > 20 * 90)) {
                d.putInt("wave", 1);
                d.putInt("next", timer + 60);
                QuestManager.say(player, "Рядовой Костя", "Движение у лесополосы! Идут!");
            }
            return;
        }
        if (d.getInt("next") > 0 && timer >= d.getInt("next")) {
            d.putInt("next", 0);
            spawnWave(level, WAVE_SIZE[Math.min(wave, WAVES) - 1]);
            player.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.25f, 0.6f);
        }
        if (timer % 20 == 0) {
            int left = enemies(level).size();
            if (left != d.getInt("left")) {
                d.putInt("left", left);
                QuestManager.updateBar(player);
            }
            if (left == 0 && d.getInt("next") == 0) {
                if (wave >= WAVES) {
                    QuestManager.setStage(player, Stage.FRONT_MEDEVAC);
                    placeWounded(level);
                    QuestManager.say(player, COMMANDER, "Отошли. Надолго ли… Студент! Ильдар на наблюдательном посту ранен, не отвечает по рации.");
                    QuestManager.say(player, COMMANDER, "Проход в проволоке левее, у столба. Вытащи его к Лене. Пригибайся.");
                } else {
                    d.putInt("wave", wave + 1);
                    d.putInt("next", timer + 200);
                    QuestManager.say(player, COMMANDER, wave == 1 ? "Отошли. Сейчас начнут бить по нам. Не высовывайся, перезарядись." : "Ещё идут! Беречь патроны!");
                    QuestManager.updateBar(player);
                }
            }
        }
        // artillery: often between the attacks, now and then during them
        int every = d.getInt("next") > 0 ? 70 : 160;
        if (timer % every == every / 2) shell(player, level, 11);
    }

    private static void spawnWave(ServerLevel level, int n) {
        for (int i = 0; i < n; i++) {
            int[] c = Plan.FRONT_COVER[RANDOM.nextInt(Plan.FRONT_COVER.length)];
            int sx = Plan.FRONT_OX + Plan.FRONT_ENEMY_X, sz = Plan.FRONT_OZ + c[1] + RANDOM.nextInt(9) - 4;
            int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
            NpcEntity e = UfaWorld.spawnNpc(level, sx + 0.5, sy, sz + 0.5, 90f, NpcRole.ENEMY, "Противник", RANDOM.nextInt(2), false);
            if (e == null) continue;
            e.setHealth(14f);
            e.setLifetime(20 * 60 * 15);
            int cx = Plan.FRONT_OX + c[0] + RANDOM.nextInt(3) - 1, cz = Plan.FRONT_OZ + c[1] + RANDOM.nextInt(3) - 1;
            e.advanceTo(new BlockPos(cx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz), cz));
        }
    }

    /** A shell lands somewhere on the field or around the trench, never right on top of the player. */
    private static void shell(ServerPlayer player, ServerLevel level, int minDist) {
        for (int tries = 0; tries < 10; tries++) {
            double x = Plan.FRONT_OX + 30 + RANDOM.nextInt(120), z = Plan.FRONT_OZ + 10 + RANDOM.nextInt(Plan.FRONT_D - 20);
            if (Math.hypot(x - player.getX(), z - player.getZ()) < minDist) continue;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);
            level.playSound(null, x, y + 20, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 3f, 0.5f);
            level.explode(null, x, y, z, 2.5f, Level.ExplosionInteraction.NONE);
            return;
        }
    }

    private static void placeWounded(ServerLevel level) {
        BlockPos op = Plan.FRONT_OP;
        if (!level.getEntitiesOfClass(NpcEntity.class, new net.minecraft.world.phys.AABB(op).inflate(6), e -> e.getRole() == NpcRole.WOUNDED).isEmpty()) return;
        UfaWorld.spawnNpc(level, op.getX() + 0.5, op.getY(), op.getZ() + 0.5, 0f, NpcRole.WOUNDED, COMRADE, 0, false);
    }

    // ------------------------------------------------------------------ the end

    private static void deliver(ServerPlayer player) {
        CompoundTag d = QuestManager.data(player);
        d.putBoolean("carry", false);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        ServerLevel level = player.serverLevel();
        BlockPos m = Plan.FRONT_MEDIC;
        NpcEntity w = UfaWorld.spawnNpc(level, m.getX() - 1.5, m.getY(), m.getZ() + 0.5, 0f, NpcRole.WOUNDED, COMRADE, 0, false);
        if (w != null) w.setLifetime(20 * 60 * 5);
        QuestManager.say(player, MEDIC, "Клади сюда. Жгут — правильно наложил, молодец. Время написал? Пиши на лбу, маркером.");
        QuestManager.setStage(player, Stage.FRONT_WOUNDED);
    }

    private static void wounded(ServerPlayer player, ServerLevel level, int timer) {
        switch (timer) {
            case 60 -> QuestManager.say(player, COMMANDER, "Студент, на выход — помоги принести ящики из хода сообщения.");
            case 100 -> {
                level.playSound(null, player.getX(), player.getY() + 18, player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 4f, 0.4f);
                QuestManager.narrate(player, "Свист. Ближе, чем все до этого.");
            }
            case 130 -> {
                // the explosion that does not kill: it is a story, not a death screen
                Vec3 p = player.position();
                level.explode(null, p.x + 7, p.y + 1, p.z, 2.0f, Level.ExplosionInteraction.NONE);
                player.setHealth(Math.max(2f, player.getHealth() - 10f));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 4, false, false));
                QuestManager.title(player, "Ранен", "", ChatFormatting.DARK_RED);
            }
            case 220 -> QuestManager.say(player, MEDIC, "Держись, слышишь? Плечо… Сейчас. Эвакуация через час, если дорогу не накроют.");
            case 320 -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0, false, false));
                QuestManager.title(player, "Госпиталь", "Месяц спустя", ChatFormatting.GRAY);
            }
            case 420 -> finish(player);
            default -> { }
        }
    }

    private static void finish(ServerPlayer player) {
        QuestManager.setStage(player, Stage.END_SVO);
        player.removeAllEffects();
        player.server.overworld().setWeatherParameters(24000, 0, false, false);
        player.server.overworld().setDayTime(6000);
        QuestManager.narrate(player, "Осколок в плечо. Эвакуация в тыл, потом поезд, потом госпиталь.");
        QuestManager.narrate(player, "Хирург на обходе показывает снимок и говорит про плечевое сплетение и подключичную артерию. Ты понимаешь каждое слово.");
        QuestManager.narrate(player, "Ильдар выжил. Сокол — нет. Из тех, с кем ты ехал в «Урале», вернулись не все.");
        QuestManager.narrate(player, "В кармане куртки — зачётка с единственной записью: «анатомия — неуд.»");
        QuestManager.title(player, "Конец", "Войну не пересдашь", ChatFormatting.DARK_RED);
        player.sendSystemMessage(Component.literal("Плохой финал. ").withStyle(ChatFormatting.RED)
                .append(QuestManager.button("Прожить день заново", "/ufa restart", ChatFormatting.YELLOW))
                .append(" ").append(QuestManager.button("Гулять по Уфе", "/ufa free", ChatFormatting.GREEN)));
        player.getInventory().add(new ItemStack(ModItems.RECORD_BOOK.get()));
    }
}
