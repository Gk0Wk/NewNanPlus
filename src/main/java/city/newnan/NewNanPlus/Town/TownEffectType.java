package city.newnan.NewNanPlus.Town;

/**
 * 小镇效果类型
 */
public enum TownEffectType {

    /**
     * 未知效果
     */
    UNKNOWN(-1, false, "unknown"),
    /**
     * 新兴小镇
     */
    BEGINNER(0, false, "beginner"),
    /**
     * 平平无奇的小镇
     */
    ORDINARY_TOWN(1, true, "ordinary_town")
    ;

    private final int ID;
    private final boolean IfDebuff;
    private final String Name;

    TownEffectType(int id, boolean ifDebuff, String name) {
        this.ID = id;
        this.IfDebuff = ifDebuff;
        this.Name = name;
    }

    public boolean getIfDebuff() {
        return this.IfDebuff;
    }

    @Override
    public String toString() {
        return this.Name;
    }

    /**
     * 从效果名称获得对应的枚举类型
     * @param EffectName 效果名称
     * @return 相应的TownEffectType枚举，没有则返回TownEffectType.UNKNOWN
     */
    public static TownEffectType fromString(String EffectName) {
        switch(EffectName) {
            case "ordinary_town":
                return ORDINARY_TOWN;
            case "beginner":
                return BEGINNER;
            default:
                return UNKNOWN;
        }
    }
}
