package ru.ufamod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import ru.ufamod.net.Net;
import ru.ufamod.net.SubmitExamPacket;
import ru.ufamod.quest.ExamQuestions;

import java.util.List;

/** The anatomy exam: one question at a time, four answers each. */
public class ExamScreen extends Screen {
    private final int[] ticket;
    private final byte[] answers;
    private int current;

    public ExamScreen(int[] ticket) {
        super(Component.literal("Экзамен по анатомии"));
        this.ticket = ticket;
        this.answers = new byte[ticket.length];
    }

    private ExamQuestions.Q question() {
        int id = ticket[current];
        return ExamQuestions.ALL.get(Math.max(0, Math.min(ExamQuestions.ALL.size() - 1, id)));
    }

    @Override
    protected void init() {
        clearWidgets();
        ExamQuestions.Q q = question();
        int w = Math.min(320, width - 40);
        int x = (width - w) / 2;
        int y = height / 2 - 10;
        for (int i = 0; i < q.options().length; i++) {
            final byte choice = (byte) i;
            String label = (char) ('А' + i) + ") " + q.options()[i];
            addRenderableWidget(Button.builder(Component.literal(label), b -> answer(choice)).bounds(x, y + i * 24, w, 20).build());
        }
    }

    private void answer(byte choice) {
        answers[current] = choice;
        current++;
        if (current >= ticket.length) {
            Net.CHANNEL.sendToServer(new SubmitExamPacket(answers));
            if (minecraft != null) minecraft.setScreen(null);
        } else {
            init();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int w = Math.min(320, width - 40);
        int x = (width - w) / 2;
        int top = height / 2 - 96;
        g.fill(x - 10, top - 8, x + w + 10, height / 2 + 96, 0xE0F4EEDC);
        g.fill(x - 10, top - 8, x + w + 10, top + 14, 0xFF7A1F1F);
        g.drawCenteredString(font, "БГМУ · Кафедра анатомии человека · Экзаменационный билет", width / 2, top - 1, 0xFFFFFF);
        g.drawString(font, "Вопрос " + (current + 1) + " из " + ticket.length, x, top + 24, 0x7A1F1F, false);
        List<FormattedCharSequence> lines = font.split(Component.literal(question().text()), w);
        int y = top + 40;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, y, 0x202020, false);
            y += 11;
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Used by the developer self-test: answers every question right or wrong and hands the ticket in. */
    public void autoAnswer(boolean correct) {
        for (int i = 0; i < ticket.length; i++) {
            int right = ExamQuestions.ALL.get(ticket[i]).correct();
            answers[i] = (byte) (correct ? right : (right + 1) % 4);
        }
        Net.CHANNEL.sendToServer(new SubmitExamPacket(answers));
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
