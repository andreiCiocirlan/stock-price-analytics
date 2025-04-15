package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

import java.util.List;

public enum PricePerformanceMilestone implements PriceMilestone {
    HIGH_52W_95("performance"),
    HIGH_4W_95("performance"),
    HIGH_ALL_TIME_95("performance"),
    LOW_52W_95("performance"),
    LOW_4W_95("performance"),
    LOW_ALL_TIME_95("performance");

    private final String type;

    PricePerformanceMilestone(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    public static String timeframeFrom(PricePerformanceMilestone milestone) {
        return switch (milestone) {
            case HIGH_52W_95, LOW_52W_95 -> "52w";
            case HIGH_4W_95, LOW_4W_95 -> "4w";
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> "all-time";
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