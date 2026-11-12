package ru.ufamod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.ufamod.UfaMod;
import ru.ufamod.item.InfoItem;
import ru.ufamod.item.ModArmorMaterials;
import ru.ufamod.item.RecordBookItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, UfaMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UfaMod.MODID);

    public static final RegistryObject<Item> LAB_COAT = ITEMS.register("lab_coat",
            () -> new ArmorItem(ModArmorMaterials.LAB_COAT, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RECORD_BOOK = ITEMS.register("record_book",
            () -> new RecordBookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SUMMONS = ITEMS.register("summons",
            () -> new InfoItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ECHPOCHMAK = ITEMS.register("echpochmak",
            () -> new InfoItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationMod(0.8f).meat().alwaysEat().build())));
    public static final RegistryObject<Item> CHAK_CHAK = ITEMS.register("chak_chak",
            () -> new InfoItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationMod(0.4f).alwaysEat().fast().build())));
    public static final RegistryObject<Item> KUMIS = ITEMS.register("kumis",
            () -> new InfoItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(3).saturationMod(0.6f).alwaysEat().build()), true));

    public static final RegistryObject<Item> CAMO_HELMET = ITEMS.register("camo_helmet",
            () -> new ArmorItem(ModArmorMaterials.CAMO, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAMO_JACKET = ITEMS.register("camo_jacket",
            () -> new ArmorItem(ModArmorMaterials.CAMO, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAMO_TROUSERS = ITEMS.register("camo_trousers",
            () -> new ArmorItem(ModArmorMaterials.CAMO, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ARMY_BOOTS = ITEMS.register("army_boots",
            () -> new ArmorItem(ModArmorMaterials.CAMO, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SAPPER_SHOVEL = ITEMS.register("sapper_shovel",
            () -> new ShovelItem(Tiers.IRON, 1.5f, -3.0f, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("ufa", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ufamod"))
            .icon(() -> RECORD_BOOK.get().getDefaultInstance())
            .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
            .build());
}
