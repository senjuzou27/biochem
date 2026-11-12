package ru.ufamod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.ufamod.UfaMod;
import ru.ufamod.quest.QuestManager;
import ru.ufamod.quest.Stage;
import ru.ufamod.world.Places;
import ru.ufamod.world.Plan;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** /ufa restart | free | leave | tp <place> | stage <name> */
@Mod.EventBusSubscriber(modid = UfaMod.MODID)
public final class UfaCommand {
    private UfaCommand() {}

    private static Map<String, BlockPos> places() {
        Places p = Places.get();
        Map<String, BlockPos> m = new LinkedHashMap<>();
        m.put("dorm", p.dormOut);
        m.put("bsmu", p.bsmuDoor);
        m.put("anatomy", p.examOut);
        m.put("library", p.library);
        m.put("gostiny", p.gostiny);
        m.put("opera", p.opera);
        m.put("park", p.park);
        m.put("artkvadrat", p.artKvadrat);
        m.put("salavat", p.salavat);
        m.put("friendship", p.friendship);
        m.put("camp", new BlockPos(Plan.CAMP_OX + 30, Plan.BASE_Y + 1, Plan.CAMP_OZ + 80));
        return m;
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("ufa")
                .then(Commands.literal("restart").executes(c -> run(c.getSource(), QuestManager::start)))
                .then(Commands.literal("start").executes(c -> run(c.getSource(), QuestManager::start)))
                .then(Commands.literal("free").executes(c -> run(c.getSource(), QuestManager::free)))
                .then(Commands.literal("leave").executes(c -> run(c.getSource(), QuestManager::leave)))
                .then(Commands.literal("tp").then(Commands.argument("place", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(places().keySet(), b))
                        .executes(c -> {
                            BlockPos pos = places().get(StringArgumentType.getString(c, "place"));
                            if (pos == null) {
                                c.getSource().sendFailure(Component.literal("Места: " + String.join(", ", places().keySet())));
                                return 0;
                            }
                            return run(c.getSource(), p -> QuestManager.teleport(p, pos));
                        })))
                .then(Commands.literal("stage").requires(s -> s.hasPermission(2)).then(Commands.argument("name", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Stage.values()).map(s -> s.name().toLowerCase()), b))
                        .executes(c -> {
                            Stage stage = Stage.byName(StringArgumentType.getString(c, "name"));
                            return run(c.getSource(), p -> QuestManager.forceStage(p, stage));
                        }))));
    }

    private interface Action {
        void apply(ServerPlayer player);
    }

    private static int run(CommandSourceStack source, Action action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Команда только для игрока"));
            return 0;
        }
        action.apply(player);
        return 1;
    }
}
