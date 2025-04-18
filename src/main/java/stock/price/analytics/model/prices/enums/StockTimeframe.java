package stock.price.analytics.model.prices.enums;

import java.util.List;

public enum StockTimeframe {

    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    public static List<StockTimeframe> higherTimeframes() {
        return List.of(WEEKLY, MONTHLY, QUARTERLY, YEARLY);
    }

    public String dbTableOHLC() {
        return switch (this) {
            case DAILY -> "daily_prices";
            case WEEKLY -> "weekly_prices";
            case MONTHLY -> "monthly_prices";
            case QUARTERLY -> "quarterly_prices";
            case YEARLY -> "yearly_prices";
        };
    }

    public String toDateTruncPeriod() {
        return switch (this) {
            case DAILY -> "DAY";
            case WEEKLY -> "WEEK";
            case MONTHLY -> "MONTH";
            case QUARTERLY -> "QUARTER";
            case YEARLY -> "YEAR";
        };
    }

    public String toIntervalPeriod() {
        return switch (this) {
            case DAILY -> "DAY";
            case WEEKLY -> "WEEK";
            case MONTHLY, QUARTERLY -> "MONTH";
            case YEARLY -> "YEAR";
        };
    }

    public String toInterval() {
        return switch (this) {
            case DAILY -> "1 DAY";
            case WEEKLY -> "1 WEEK";
            case MONTHLY -> "1 MONTH";
            case QUARTERLY -> "3 MONTH";
            case YEARLY -> "1 YEAR";
        };
    }

    public String stockPrefix() {
        return switch (this) {
            case DAILY -> "d_";
            case WEEKLY -> "w_";
            case MONTHLY -> "m_";
            case QUARTERLY -> "q_";
            case YEARLY -> "y_";
        };
    }
}