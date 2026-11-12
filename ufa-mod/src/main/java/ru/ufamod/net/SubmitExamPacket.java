package ru.ufamod.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import ru.ufamod.quest.QuestManager;

import java.util.function.Supplier;

/** Client -> server: the chosen answers, one byte per question. */
public record SubmitExamPacket(byte[] answers) {
    public static void encode(SubmitExamPacket msg, FriendlyByteBuf buf) {
        buf.writeByteArray(msg.answers);
    }

    public static SubmitExamPacket decode(FriendlyByteBuf buf) {
        return new SubmitExamPacket(buf.readByteArray(16));
    }

    public static void handle(SubmitExamPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) QuestManager.onExamSubmitted(player, msg.answers);
        });
        ctx.get().setPacketHandled(true);
    }
}
