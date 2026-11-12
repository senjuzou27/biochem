package ru.ufamod.quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** The anatomy exam: shared by the server (grading) and the client (the exam screen). */
public final class ExamQuestions {
    private ExamQuestions() {}

    public record Q(String text, String[] options, int correct) {}

    public static final int TICKET_SIZE = 5;
    public static final int PASS_SCORE = 3;

    public static final List<Q> ALL = List.of(
            q("Сколько зубов в постоянном прикусе взрослого человека?", 2, "20", "28", "32", "36"),
            q("Сколько молочных зубов у ребёнка?", 1, "16", "20", "24", "28"),
            q("Какая кость черепа подвижна?", 3, "Верхняя челюсть", "Скуловая", "Височная", "Нижняя челюсть"),
            q("Какой нерв обеспечивает чувствительность зубов?", 0, "Тройничный (V пара)", "Лицевой (VII пара)", "Блуждающий (X пара)", "Подъязычный (XII пара)"),
            q("Самая твёрдая ткань человеческого организма — это…", 1, "Дентин", "Эмаль", "Цемент зуба", "Компактная кость"),
            q("Сколько пар черепных нервов у человека?", 2, "10", "11", "12", "31"),
            q("Сколько позвонков в шейном отделе позвоночника?", 0, "7", "5", "12", "8"),
            q("Как по-латыни называется нижняя челюсть?", 1, "Maxilla", "Mandibula", "Os zygomaticum", "Vomer"),
            q("Какая из этих мышц — жевательная и поднимает нижнюю челюсть?", 3, "M. buccinator", "M. orbicularis oris", "M. platysma", "M. masseter"),
            q("Сколько камер в сердце человека?", 2, "2", "3", "4", "5"),
            q("Самая длинная кость тела человека — это…", 0, "Бедренная", "Большеберцовая", "Плечевая", "Малоберцовая"),
            q("Сколько пар рёбер у человека?", 1, "10", "12", "14", "7"),
            q("Как называется первый шейный позвонок?", 2, "Аксис", "Эпистрофей", "Атлант", "Копчик"),
            q("Сколько корней обычно у первого верхнего моляра?", 2, "1", "2", "3", "4"),
            q("Мягкая ткань внутри зуба с сосудами и нервами — это…", 0, "Пульпа", "Периодонт", "Дентин", "Десна"),
            q("Через какое отверстие черепа полость черепа сообщается с позвоночным каналом?", 3, "Овальное", "Круглое", "Яремное", "Большое затылочное"),
            q("Самая крупная слюнная железа — это…", 1, "Поднижнечелюстная", "Околоушная", "Подъязычная", "Нёбная"),
            q("Сколько костей в скелете взрослого человека (примерно)?", 0, "206", "180", "250", "300"),
            q("Височно-нижнечелюстной сустав образован нижней челюстью и … костью", 2, "скуловой", "клиновидной", "височной", "затылочной"),
            q("Какая ветвь тройничного нерва иннервирует нижние зубы?", 3, "Глазной нерв (I ветвь)", "Верхнечелюстной нерв (II ветвь)", "Ни одна из них", "Нижнечелюстной нерв (III ветвь)")
    );

    private static Q q(String text, int correct, String... options) {
        return new Q(text, options, correct);
    }

    public static int[] ticket(Random random) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < ALL.size(); i++) ids.add(i);
        Collections.shuffle(ids, random);
        int[] out = new int[TICKET_SIZE];
        for (int i = 0; i < TICKET_SIZE; i++) out[i] = ids.get(i);
        return out;
    }

    public static int score(int[] ticket, byte[] answers) {
        int score = 0;
        for (int i = 0; i < ticket.length && i < answers.length; i++)
            if (ticket[i] >= 0 && ticket[i] < ALL.size() && ALL.get(ticket[i]).correct() == answers[i]) score++;
        return score;
    }
}
