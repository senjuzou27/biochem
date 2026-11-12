package ru.ufamod.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import ru.ufamod.client.ClientHooks;

import java.util.function.Supplier;

/** Server -> client: here is your exam ticket (indices into the question pool). */
public record OpenExamPacket(int[] ticket) {
    public static void encode(OpenExamPacket msg, FriendlyByteBuf buf) {
        buf.writeVarIntArray(msg.ticket);
    }

    public static OpenExamPacket decode(FriendlyByteBuf buf) {
        return new OpenExamPacket(buf.readVarIntArray(16));
    }

    public static void handle(OpenExamPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openExam(msg.ticket)));
        ctx.get().setPacketHandled(true);
    }
}
