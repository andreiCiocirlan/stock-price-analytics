package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

import java.util.List;

public enum PricePerformanceMilestone implements PriceMilestone {
    NEW_52W_HIGH,
    NEW_4W_HIGH,
    NEW_ALL_TIME_HIGH,
    NEW_52W_LOW,
    NEW_4W_LOW,
    NEW_ALL_TIME_LOW,
    HIGH_52W_95,
    HIGH_4W_95,
    HIGH_ALL_TIME_95,
    LOW_52W_95,
    LOW_4W_95,
    LOW_ALL_TIME_95,
    NONE;

    public static String timeframeFrom(PricePerformanceMilestone milestone) {
        return switch (milestone) {
            case NEW_52W_LOW, NEW_52W_HIGH, HIGH_52W_95, LOW_52W_95 -> "52w";
            case NEW_4W_LOW, NEW_4W_HIGH, HIGH_4W_95, LOW_4W_95 -> "4w";
            case NEW_ALL_TIME_LOW, NEW_ALL_TIME_HIGH, HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> "all-time";
            case NONE -> throw new IllegalStateException("Unexpected milestone NONE");
        };
    }

    public static List<PricePerformanceMilestone> milestones95thPercentile() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95, LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95);
    }

    public static List<PricePerformanceMilestone> high95thPercentileValues() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95);
    }

    public static List<PricePerformanceMilestone> low95thPercentileValues() {
        return List.of(LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95);
    }
}