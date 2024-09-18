package stock.price.analytics.model.prices.enums;

public enum HighLowPeriod {

    HIGH_LOW_4W,
    HIGH_LOW_52W,
    HIGH_LOW_ALL_TIME;

    public static String intervalFrom(HighLowPeriod period) {
        return switch (period) {
            case HIGH_LOW_4W -> "4";
            case HIGH_LOW_52W -> "52";
            case HIGH_LOW_ALL_TIME -> "UNBOUNDED";
        };
    }

    public static String tableNameFrom(HighLowPeriod period) {
        return switch (period) {
            case HIGH_LOW_4W -> "high_low4w";
            case HIGH_LOW_52W -> "high_low52w";
            case HIGH_LOW_ALL_TIME -> "highest_lowest_prices";
        };
    }

    public static String whereClauseFrom(HighLowPeriod period, String date) {
        return switch (period) {
            case HIGH_LOW_4W -> STR."wp.start_date >= DATE_TRUNC('week', '\{date}'::date) - INTERVAL '4 week'";
            case HIGH_LOW_52W -> STR."wp.start_date >= DATE_TRUNC('week', '\{date}'::date) - INTERVAL '52 week'";
            case HIGH_LOW_ALL_TIME -> "1=1";
        };
    }

    public static String sequenceNameFrom(HighLowPeriod period) {
        return switch (period) {
            case HIGH_LOW_4W, HIGH_LOW_52W, HIGH_LOW_ALL_TIME -> "sequence_high_low";
        };
    }


}
