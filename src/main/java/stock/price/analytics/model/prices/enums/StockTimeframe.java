package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;
import stock.price.analytics.model.prices.ohlc.MonthlyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.YearlyPriceOHLC;

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

    public static Class<? extends AbstractPriceOHLC> tableClassFrom(StockTimeframe timeframe) {
        return switch (timeframe) {
            case WEEKLY -> WeeklyPriceOHLC.class;
            case MONTHLY -> MonthlyPriceOHLC.class;
            case YEARLY -> YearlyPriceOHLC.class;
        };
    }
}