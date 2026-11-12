package ru.ufamod.worldgen;

import java.util.Random;

final class Names {
    private Names() {}

    private static final String[] CITIZENS = {"Айгуль", "Рустам", "Гульнара", "Ильдар", "Алсу", "Азат", "Лилия", "Тимур", "Зульфия",
            "Радик", "Ольга", "Сергей", "Наталья", "Булат", "Эльвира", "Марат", "Диана", "Артур", "Регина", "Вадим", "Ринат", "Гузель",
            "Айдар", "Земфира", "Салават", "Лейсан", "Динар", "Альфия", "Ирек", "Светлана"};

    static String citizen(Random r) {
        return CITIZENS[r.nextInt(CITIZENS.length)];
    }
}
