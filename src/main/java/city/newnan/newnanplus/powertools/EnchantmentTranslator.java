package city.newnan.newnanplus.powertools;

import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;

public class EnchantmentTranslator {
    public String of(Enchantment enchantment) {
        return translationMap.get(enchantment);
    }
    private static final HashMap<Enchantment, String> translationMap = new HashMap<>() {{
        put(Enchantment.ARROW_DAMAGE            , "力量");
        put(Enchantment.ARROW_FIRE              , "火矢");
        put(Enchantment.ARROW_INFINITE          , "无限");
        put(Enchantment.ARROW_KNOCKBACK         , "冲击");
        put(Enchantment.BINDING_CURSE           , "绑定诅咒");
        put(Enchantment.CHANNELING              , "引雷");
        put(Enchantment.DAMAGE_ALL              , "锋利");
        put(Enchantment.DAMAGE_ARTHROPODS       , "节肢杀手");
        put(Enchantment.DAMAGE_UNDEAD           , "亡灵杀手");
        put(Enchantment.DEPTH_STRIDER           , "深海探索者");
        put(Enchantment.DIG_SPEED               , "效率");
        put(Enchantment.DURABILITY              , "耐久");
        put(Enchantment.FIRE_ASPECT             , "火焰附加");
        put(Enchantment.FROST_WALKER            , "冰霜行者");
        put(Enchantment.IMPALING                , "穿刺");
        put(Enchantment.KNOCKBACK               , "击退");
        put(Enchantment.LOOT_BONUS_BLOCKS       , "时运");
        put(Enchantment.LOOT_BONUS_MOBS         , "抢夺");
        put(Enchantment.LOYALTY                 , "忠诚");
        put(Enchantment.LUCK                    , "海之眷顾");
        put(Enchantment.LURE                    , "饵钓");
        put(Enchantment.MENDING                 , "经验修补");
        put(Enchantment.MULTISHOT               , "多重射击");
        put(Enchantment.OXYGEN                  , "水下呼吸");
        put(Enchantment.PIERCING                , "穿透");
        put(Enchantment.PROTECTION_ENVIRONMENTAL, "保护");
        put(Enchantment.PROTECTION_EXPLOSIONS   , "爆炸保护");
        put(Enchantment.PROTECTION_FALL         , "摔落保护");
        put(Enchantment.PROTECTION_FIRE         , "火焰保护");
        put(Enchantment.PROTECTION_PROJECTILE   , "弹射物保护");
        put(Enchantment.QUICK_CHARGE            , "快速装填");
        put(Enchantment.RIPTIDE                 , "激流");
        put(Enchantment.SILK_TOUCH              , "精准采集");
        // put(Enchantment.SOUL_SPEED              , "灵魂疾行");
        put(Enchantment.SWEEPING_EDGE           , "横扫之刃");
        put(Enchantment.THORNS                  , "荆棘");
        put(Enchantment.VANISHING_CURSE         , "消失诅咒");
        put(Enchantment.WATER_WORKER            , "水下速掘");
    }};
}
