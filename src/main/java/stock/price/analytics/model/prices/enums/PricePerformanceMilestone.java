package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.StockPriceMilestone;
import stock.price.analytics.model.prices.context.StockHighLowForPeriodContext;

import java.util.List;

public enum PricePerformanceMilestone implements StockPriceMilestone<StockHighLowForPeriodContext> {
    HIGH_52W_95,
    HIGH_4W_95,
    HIGH_ALL_TIME_95,
    LOW_52W_95,
    LOW_4W_95,
    LOW_ALL_TIME_95,
    HIGH_52W_90,
    HIGH_4W_90,
    HIGH_ALL_TIME_90,
    LOW_52W_90,
    LOW_4W_90,
    LOW_ALL_TIME_90;

    public static String timeframeFrom(PricePerformanceMilestone milestone) {
        return switch (milestone) {
            case HIGH_52W_95, LOW_52W_95, HIGH_52W_90, LOW_52W_90 -> "52w";
            case HIGH_4W_95, LOW_4W_95, HIGH_4W_90, LOW_4W_90 -> "4w";
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95, HIGH_ALL_TIME_90, LOW_ALL_TIME_90 -> "all-time";
        };
    }

    public boolean isLowPercentile() {
        return List.of(LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95, LOW_52W_90, LOW_4W_90, LOW_ALL_TIME_90).contains(this);
    }

    public boolean isHighPercentile() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95, HIGH_52W_90, HIGH_4W_90, HIGH_ALL_TIME_90).contains(this);
    }

    public boolean isLow95thPercentile() {
        return List.of(LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95).contains(this);
    }

    public boolean isHigh95thPercentile() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95).contains(this);
    }

    public boolean is95thPercentileValue() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95, LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95).contains(this);
    }

    public static List<PricePerformanceMilestone> highPercentileValues() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95, HIGH_52W_90, HIGH_4W_90, HIGH_ALL_TIME_90);
    }

    @Override
    public boolean isMetFor(StockHighLowForPeriodContext context) {
        double latestPrice = context.stock().getClose();
        double low = context.highLowForPeriod().getLow();
        double high = context.highLowForPeriod().getHigh();
        return switch (this) {
            case HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95 -> latestPrice >= high * 0.95 && latestPrice <= high;
            case HIGH_52W_90, HIGH_4W_90, HIGH_ALL_TIME_90 -> latestPrice >= high * 0.90 && latestPrice <= high;
            case LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95 -> latestPrice >= low && latestPrice <= low * 1.05;
            case LOW_52W_90, LOW_4W_90, LOW_ALL_TIME_90 -> latestPrice >= low && latestPrice <= low * 1.10;
        };
    }
}