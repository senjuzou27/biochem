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
import ru.ufamod.world.Plan;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** /ufa restart | free | leave | tp <place> | stage <name> */
@Mod.EventBusSubscriber(modid = UfaMod.MODID)
public final class UfaCommand {
    private UfaCommand() {}

    private static final Map<String, BlockPos> PLACES = new LinkedHashMap<>();

    static {
        PLACES.put("dorm", Plan.w(337, 1, 38));
        PLACES.put("bsmu", Plan.w(326, 1, 386));
        PLACES.put("gostiny", Plan.w(300, 1, 134));
        PLACES.put("opera", Plan.w(326, 1, 280));
        PLACES.put("park", Plan.w(384, 1, 462));
        PLACES.put("salavat", Plan.w(70, 1, 596));
        PLACES.put("friendship", Plan.w(598, 1, 598));
        PLACES.put("sovetskaya", Plan.w(480, 1, 340));
        PLACES.put("camp", new BlockPos(Plan.CAMP_OX + 30, Plan.BASE_Y + 1, Plan.CAMP_OZ + 80));
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
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(PLACES.keySet(), b))
                        .executes(c -> {
                            BlockPos pos = PLACES.get(StringArgumentType.getString(c, "place"));
                            if (pos == null) {
                                c.getSource().sendFailure(Component.literal("Места: " + String.join(", ", PLACES.keySet())));
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
