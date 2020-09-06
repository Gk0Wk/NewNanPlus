package city.newnan.NewNanPlus.Town;

/**
 * 小镇资源种类
 */
public enum ResourceType {
    /**
     * 未知
     */
    UNKNOWN,
    /**
     * 木材
     */
    WOOD,
    /**
     * 石材
     */
    STONE,
    /**
     * 煤炭
     */
    COAL,
    /**
     * 铁
     */
    IRON,
    /**
     * 金
     */
    GOLD,
    /**
     * 青金石
     */
    LAPIS,
    /**
     * 红石
     */
    REDSTONE,
    /**
     * 钻石
     */
    DIAMOND,
    /**
     * 绿宝石
     */
    EMERALD,
    /**
     * 肉类
     */
    MEAT,
    /**
     * 种子
     */
    SEED,
    /**
     * 农作物
     */
    CROP,
    /**
     * 书籍
     */
    BOOK,
    /**
     * 工具
     */
    TOOL;

    @Override
    public String toString() {
        switch (this) {
            case WOOD:
                return "wood";
            case STONE:
                return "stone";
            case COAL:
                return "coal";
            case IRON:
                return "iron";
            case GOLD:
                return "gold";
            case LAPIS:
                return "lapis";
            case REDSTONE:
                return "redstone";
            case DIAMOND:
                return "diamond";
            case EMERALD:
                return "emerald";
            case MEAT:
                return "meat";
            case SEED:
                return "seed";
            case CROP:
                return "crop";
            case BOOK:
                return "book";
            case TOOL:
                return "tool";
            default:
                return "undefined";
        }
    }

    public static String toString(ResourceType Type) {
        return Type.toString();
    }

    public static ResourceType fromString(String TypeName) {
        switch(TypeName.toLowerCase()) {
            case "wood":
                return WOOD;
            case "stone":
                return STONE;
            case "coal":
                return COAL;
            case "iron":
                return IRON;
            case "gold":
                return GOLD;
            case "lapis":
                return LAPIS;
            case "redstone":
                return REDSTONE;
            case "diamond":
                return DIAMOND;
            case "emerald":
                return EMERALD;
            case "meat":
                return MEAT;
            case "seed":
                return SEED;
            case "book":
                return BOOK;
            case "tool":
                return TOOL;
            default:
                return UNKNOWN;
        }
    }
}
