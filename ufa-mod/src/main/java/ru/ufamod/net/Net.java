package ru.ufamod.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ru.ufamod.UfaMod;

public final class Net {
    private Net() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(UfaMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OpenExamPacket.class, OpenExamPacket::encode, OpenExamPacket::decode, OpenExamPacket::handle);
        CHANNEL.registerMessage(1, SubmitExamPacket.class, SubmitExamPacket::encode, SubmitExamPacket::decode, SubmitExamPacket::handle);
    }
}
