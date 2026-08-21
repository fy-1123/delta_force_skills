package mccfk.DeltaForce.java.Eeconomy;

/**
 * 货币类型枚举
 * 定义DF模组的经济系统中所有支持的货币种类
 */
public enum CurrencyType {

    HAVOC_COIN("havoc_coin", " 哈夫币"),
    DELTA_COIN("delta_coin", " 三角币");

    private final String id;
    private final String displayName;

    CurrencyType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据ID字符串查找对应的货币类型
     * @param id 货币ID（如 "havoc_coin"）
     * @return 匹配的CurrencyType，未找到返回null
     */
    public static CurrencyType fromId(String id) {
        for (CurrencyType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
