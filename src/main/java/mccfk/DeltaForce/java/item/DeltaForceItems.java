package mccfk.DeltaForce.java.item;

import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * DeltaForce物品注册 - 所有三角洲行动(DF)相关物品
 * 品质等级通过描述文件 .level(X); 决定，不在此处硬编码
 */
public class DeltaForceItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DeltaForce.MODID);

    // 临时测试物品 - modelsnull 占位模型
    public static final RegistryObject<Item> TEMP_DF = plainItem("temp_df");

    public static final RegistryObject<Item> LOGO_DF = plainItem("logo_df");

    // 6级(红色)
    public static final RegistryObject<Item> AFRICAN_STAR = plainItem("african_star");

    public static final RegistryObject<Item> OCEAN_TEAR = plainItem("ocean_tear");

    public static final RegistryObject<Item> EXPERIMENTAL_DATA = plainItem("experimental_data");

    public static final RegistryObject<Item> CARBON_FIBER_PLATE = plainItem("carbon_fiber_plate");

    public static final RegistryObject<Item> GOLD_BAR = plainItem("gold_bar");

    public static final RegistryObject<Item> HIGH_SPEED_DISK_ARRAY = plainItem("high_speed_disk_array");

    // 5级(金色)
    public static final RegistryObject<Item> GOLD_COIN = plainItem("gold_coin");

    public static final RegistryObject<Item> IMPRESSIONIST_PAINTING = plainItem("impressionist_painting");

    public static final RegistryObject<Item> ASARA_CLASSIFIED_DOCS = plainItem("asara_classified_docs");

    // 3级(蓝色)
    public static final RegistryObject<Item> SUGAR_TRIANGLE = plainItem("sugar_triangle");

    public static final RegistryObject<Item> SILVER_COIN = plainItem("silver_coin");

    // 2级(绿色)
    public static final RegistryObject<Item> COPPER_COIN = plainItem("copper_coin");

    // 1级(白色)
    public static final RegistryObject<Item> LITTLE_JOKER = plainItem("little_joker");

    // ===== 调酒原材料（从CFK移植，移除拼音缩写，使用规范英文名）=====

    // 杜松灵
    public static final RegistryObject<Item> JUNIPER_ESSENCE = plainItem("juniper_essence");

    // 清逸饮
    public static final RegistryObject<Item> REFRESHING_DRINK = plainItem("refreshing_drink");

    // 琥珀露
    public static final RegistryObject<Item> AMBER_DEW = plainItem("amber_dew");

    // 蜜糖汁
    public static final RegistryObject<Item> HONEY_JUICE = plainItem("honey_juice");

    // 蔓越莓汁
    public static final RegistryObject<Item> CRANBERRY_JUICE = plainItem("cranberry_juice");

    // 柠檬汁
    public static final RegistryObject<Item> LEMON_JUICE = plainItem("lemon_juice");

    // 橙汁
    public static final RegistryObject<Item> ORANGE_JUICE = plainItem("orange_juice");

    // 可乐
    public static final RegistryObject<Item> COLA = plainItem("cola");

    // 糖浆
    public static final RegistryObject<Item> SYRUP = plainItem("syrup");

    // 苏打水
    public static final RegistryObject<Item> SODA_WATER = plainItem("soda_water");

    // 汤力水
    public static final RegistryObject<Item> TONIC_WATER = plainItem("tonic_water");

    /**
     * 注册普通物品（品质由描述文件.level(X);决定）
     */
    private static RegistryObject<Item> plainItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /**
     * 判断是否为调酒原材料（仅掉落物形态时使用 modelsnull 模型）
     */
    public static boolean isRawMaterial(ItemStack stack) {
        return stack.is(JUNIPER_ESSENCE.get())
            || stack.is(REFRESHING_DRINK.get())
            || stack.is(AMBER_DEW.get())
            || stack.is(HONEY_JUICE.get())
            || stack.is(CRANBERRY_JUICE.get())
            || stack.is(LEMON_JUICE.get())
            || stack.is(ORANGE_JUICE.get())
            || stack.is(COLA.get())
            || stack.is(SYRUP.get())
            || stack.is(SODA_WATER.get())
            || stack.is(TONIC_WATER.get());
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}