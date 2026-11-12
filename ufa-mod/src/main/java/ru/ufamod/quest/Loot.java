package ru.ufamod.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ru.ufamod.registry.ModItems;

import java.util.List;

/** Contents of the chests of the story and the special item stacks. */
public final class Loot {
    private Loot() {}

    public static List<ItemStack> of(String key) {
        return switch (key) {
            case "wardrobe" -> List.of(new ItemStack(ModItems.LAB_COAT.get()), new ItemStack(ModItems.RECORD_BOOK.get()), atlas());
            case "fridge" -> List.of(new ItemStack(ModItems.ECHPOCHMAK.get(), 2), new ItemStack(ModItems.KUMIS.get()), new ItemStack(ModItems.CHAK_CHAK.get()));
            case "canteen" -> List.of(new ItemStack(ModItems.ECHPOCHMAK.get(), 3), new ItemStack(ModItems.CHAK_CHAK.get(), 2), new ItemStack(ModItems.KUMIS.get(), 2));
            default -> List.of();
        };
    }

    private static final String[] ATLAS_PAGES = {
            "АТЛАС АНАТОМИИ\nчеловека\n\nКонспект к экзамену.\nСтоматологический\nфакультет БГМУ.\n\nЧитай внимательно:\nв билете 5 вопросов,\nдля «удовл.» нужно\n3 верных ответа.",
            "ЗУБЫ\n\nПостоянных зубов — 32.\nМолочных — 20.\n\nЭмаль — самая твёрдая\nткань организма.\n\nПульпа — мягкая ткань\nвнутри зуба с сосудами\nи нервами.",
            "ЗУБЫ (продолж.)\n\nПервый верхний моляр\nобычно имеет 3 корня.\n\nЗубы иннервирует\nтройничный нерв\n(V пара).\nНижние зубы — его\nIII ветвь: нижне-\nчелюстной нерв.",
            "ЧЕРЕП\n\nЕдинственная подвижная\nкость черепа — нижняя\nчелюсть (mandibula).\nВерхняя — maxilla.\n\nВНЧС образован нижней\nчелюстью и височной\nкостью.",
            "ЧЕРЕП (продолж.)\n\nПолость черепа\nсообщается с позво-\nночным каналом через\nбольшое затылочное\nотверстие.\n\nЧерепных нервов —\n12 пар.",
            "МЫШЦЫ И ЖЕЛЕЗЫ\n\nНижнюю челюсть\nподнимает жевательная\nмышца — m. masseter.\n\nСамая крупная слюнная\nжелеза — околоушная.",
            "СКЕЛЕТ\n\nКостей у взрослого —\nоколо 206.\nШейных позвонков — 7.\nПервый — атлант,\nвторой — аксис.\nРёбер — 12 пар.\nСамая длинная кость —\nбедренная.",
            "СЕРДЦЕ\n\nКамер четыре: два\nпредсердия и два\nжелудочка.\n\n\nНи пуха ни пера!"
    };

    /** The anatomy atlas: a written book holding the answers to every exam question. */
    public static ItemStack atlas() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "Атлас анатомии");
        tag.putString("author", "Кафедра анатомии БГМУ");
        tag.putBoolean("resolved", true);
        ListTag pages = new ListTag();
        for (String page : ATLAS_PAGES) pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
        tag.put("pages", pages);
        return book;
    }

    /** The entrenching tool that may only dig earth (the player is in adventure mode). */
    public static ItemStack sapperShovel() {
        ItemStack shovel = new ItemStack(ModItems.SAPPER_SHOVEL.get());
        ListTag can = new ListTag();
        for (String id : new String[]{"minecraft:dirt", "minecraft:coarse_dirt", "minecraft:grass_block"}) can.add(StringTag.valueOf(id));
        shovel.getOrCreateTag().put("CanDestroy", can);
        return shovel;
    }
}
