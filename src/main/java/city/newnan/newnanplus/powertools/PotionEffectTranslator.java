package city.newnan.newnanplus.powertools;

import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;

public class PotionEffectTranslator {
    public String of(PotionEffectType potionEffectType) {
        return translationMap.get(potionEffectType);
    }
    private static final HashMap<PotionEffectType, String> translationMap = new HashMap<PotionEffectType, String>() {{
        put(PotionEffectType.ABSORPTION         , "伤害吸收");
        put(PotionEffectType.BAD_OMEN           , "不祥之兆");
        put(PotionEffectType.BLINDNESS          , "失明");
        put(PotionEffectType.CONDUIT_POWER      , "潮涌能量");
        put(PotionEffectType.CONFUSION          , "反胃");
        put(PotionEffectType.DAMAGE_RESISTANCE  , "抗性提升");
        put(PotionEffectType.DOLPHINS_GRACE     , "海豚的恩惠");
        put(PotionEffectType.FAST_DIGGING       , "急迫");
        put(PotionEffectType.FIRE_RESISTANCE    , "防火");
        put(PotionEffectType.GLOWING            , "发光");
        put(PotionEffectType.HARM               , "伤害");
        put(PotionEffectType.HEAL               , "治愈");
        put(PotionEffectType.HEALTH_BOOST       , "生命提升");
        put(PotionEffectType.HERO_OF_THE_VILLAGE, "村庄英雄");
        put(PotionEffectType.HUNGER             , "饥饿");
        put(PotionEffectType.INCREASE_DAMAGE    , "力量");
        put(PotionEffectType.INVISIBILITY       , "隐身");
        put(PotionEffectType.JUMP               , "跳跃");
        put(PotionEffectType.LEVITATION         , "飘浮");
        put(PotionEffectType.LUCK               , "幸运");
        put(PotionEffectType.NIGHT_VISION       , "夜视");
        put(PotionEffectType.POISON             , "中毒");
        put(PotionEffectType.REGENERATION       , "生命恢复");
        put(PotionEffectType.SATURATION         , "饱和");
        put(PotionEffectType.SLOW               , "缓慢");
        put(PotionEffectType.SLOW_DIGGING       , "挖掘疲劳");
        put(PotionEffectType.SLOW_FALLING       , "缓降");
        put(PotionEffectType.SPEED              , "速度");
        put(PotionEffectType.UNLUCK             , "霉运");
        put(PotionEffectType.WATER_BREATHING    , "水下呼吸");
        put(PotionEffectType.WEAKNESS           , "虚弱");
        put(PotionEffectType.WITHER             , "凋零");
    }};
}
