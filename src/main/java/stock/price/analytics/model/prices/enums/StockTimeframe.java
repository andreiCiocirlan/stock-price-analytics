package stock.price.analytics.model.prices.enums;

public enum StockTimeframe {

    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static String dbTablePerfHeatmapFrom(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> "daily_prices_performance_view";
            case WEEKLY -> "weekly_prices_performance_view";
            case MONTHLY -> "monthly_prices_performance_view";
            case YEARLY -> "yearly_prices_performance_view";
        };
    }

    public static String dbTableOHLCFrom(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> "daily_prices";
            case WEEKLY -> "weekly_prices";
            case MONTHLY -> "monthly_prices";
            case YEARLY -> "yearly_prices";
        };
    }
}