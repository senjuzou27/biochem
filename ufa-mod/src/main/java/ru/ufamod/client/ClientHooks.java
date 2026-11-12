package ru.ufamod.client;

import net.minecraft.client.Minecraft;

/** Entry points called from packet handlers; only ever loaded on the physical client. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openExam(int[] ticket) {
        Minecraft.getInstance().setScreen(new ExamScreen(ticket));
    }
}
