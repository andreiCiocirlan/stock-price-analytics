package stock.price.analytics.model.prices.enums;

import stock.price.analytics.model.prices.PriceMilestone;

import java.util.List;

public enum PricePerformanceMilestone implements PriceMilestone {
    HIGH_52W_95("performance"),
    HIGH_4W_95("performance"),
    HIGH_ALL_TIME_95("performance"),
    LOW_52W_95("performance"),
    LOW_4W_95("performance"),
    LOW_ALL_TIME_95("performance"),
    HIGH_52W_90("performance"),
    HIGH_4W_90("performance"),
    HIGH_ALL_TIME_90("performance"),
    LOW_52W_90("performance"),
    LOW_4W_90("performance"),
    LOW_ALL_TIME_90("performance");

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
            case HIGH_52W_95, LOW_52W_95, HIGH_52W_90, LOW_52W_90 -> "52w";
            case HIGH_4W_95, LOW_4W_95, HIGH_4W_90, LOW_4W_90 -> "4w";
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95, HIGH_ALL_TIME_90, LOW_ALL_TIME_90 -> "all-time";
        };
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
}