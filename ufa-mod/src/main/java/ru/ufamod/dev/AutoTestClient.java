package ru.ufamod.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import ru.ufamod.UfaMod;
import ru.ufamod.client.ExamScreen;

/** Client half of {@link AutoTest}: takes the screenshots and answers the exam. */
public final class AutoTestClient {
    private int quitTimer = -1;
    private int menuTicks;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ExamScreen exam && AutoTest.examMode != null && !AutoTest.examMode.equals("show")) {
            exam.autoAnswer(AutoTest.examMode.equals("pass"));
            AutoTest.examMode = null;
        }
        // Click through the one-off "experimental world settings" / backup question when the test world is opened.
        if (mc.level == null && mc.screen != null && ++menuTicks % 40 == 0) {
            for (var child : mc.screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button button
                        && button.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents key
                        && (key.getKey().equals("selectWorld.backupJoinSkipButton") || key.getKey().equals("gui.proceed") || key.getKey().equals("gui.yes"))) {
                    UfaMod.LOGGER.info("AUTOTEST pressing '{}' on {}", key.getKey(), mc.screen.getClass().getSimpleName());
                    button.onPress();
                    break;
                }
            }
        }
        String shot = AutoTest.pendingShot;
        if (shot != null && mc.level != null) {
            Screenshot.grab(mc.gameDirectory, shot + ".png", mc.getMainRenderTarget(), msg -> UfaMod.LOGGER.info("AUTOTEST {}", msg.getString()));
            AutoTest.pendingShot = null;
        }
        if (AutoTest.finished && quitTimer < 0) quitTimer = 60;
        if (quitTimer > 0 && --quitTimer == 0) mc.stop();
    }
}
