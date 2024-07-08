package stock.price.analytics.model.prices.enums;

public enum StockTimeframe {

    WEEKLY,
    MONTHLY,
    YEARLY;

    public static String dbTableForPerfHeatmapFrom(StockTimeframe timeframe) {
        return switch (timeframe) {
            case WEEKLY -> "weekly_prices_performance_view";
            case MONTHLY -> "monthly_prices_performance_view";
            case YEARLY -> "yearly_prices_performance_view";
        };
    }

}