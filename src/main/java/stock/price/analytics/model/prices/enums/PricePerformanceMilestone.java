package stock.price.analytics.model.prices.enums;

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

    public static List<PricePerformanceMilestone> milestones95thPercentile() {
        return List.of(HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95, LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95);
    }

}