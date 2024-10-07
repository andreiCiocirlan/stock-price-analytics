package stock.price.analytics.model.prices.enums;

public enum PriceMilestone {
    NEW_52W_HIGH,
    NEW_4W_HIGH,
    NEW_ALL_TIME_HIGH,
    NEW_52W_LOW,
    NEW_4W_LOW,
    NEW_ALL_TIME_LOW,
    NONE;

    public static String tableNameFrom(PriceMilestone priceMilestone) {
        return switch (priceMilestone) {
            case NEW_52W_HIGH, NEW_52W_LOW -> "high_low52w";
            case NEW_4W_HIGH, NEW_4W_LOW -> "high_low4w";
            case NEW_ALL_TIME_HIGH, NEW_ALL_TIME_LOW -> "highest_lowest";
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

    public static String whereClauseFrom(PriceMilestone priceMilestone) {
        return switch (priceMilestone) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH -> "wp.high > hl_prev.high";
            case NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW -> "wp.low < hl_prev.low";
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }
}