package stock.price.analytics.model.prices.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public enum PriceMilestone {
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

    public String tableName() {
        return switch (this) {
            case NEW_52W_HIGH, NEW_52W_LOW, HIGH_52W_95, LOW_52W_95 -> "high_low52w";
            case NEW_4W_HIGH, NEW_4W_LOW, HIGH_4W_95, LOW_4W_95 -> "high_low4w";
            case NEW_ALL_TIME_HIGH, NEW_ALL_TIME_LOW, HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> "highest_lowest";
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

    public String whereClause() {
        return switch (this) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH -> "wp.high > hl.high";
            case NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW -> "wp.low < hl.low";
            case HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95 -> "((hl.high <> hl.low) AND (1 - (1 - (wp.close - hl.low)/(hl.high - hl.low))) > 0.95 )";
            case LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95 -> "((hl.high <> hl.low) AND (1 - (wp.close - hl.low)/(hl.high - hl.low)) > 0.95 )";
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

    public String joinDate() {
        return switch (this) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH, NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW ->
                    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                            .minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95, LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95 ->
                    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(DateTimeFormatter.ISO_LOCAL_DATE);
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }
}