package stock.price.analytics.model.prices.enums;

public enum HighLowPeriod {

    HIGH_LOW_4W,
    HIGH_LOW_52W,
    HIGH_LOW_ALL_TIME;

    public String intervalPreceding() {
        return switch (this) {
            case HIGH_LOW_4W -> "3"; // between 3 preceding and current row -> means previous 4-week period
            case HIGH_LOW_52W -> "51"; // between 51 preceding and current row -> means previous 52-week period
            case HIGH_LOW_ALL_TIME -> "UNBOUNDED";
        };
    }

    public String tableName() {
        return switch (this) {
            case HIGH_LOW_4W -> "high_low4w";
            case HIGH_LOW_52W -> "high_low52w";
            case HIGH_LOW_ALL_TIME -> "highest_lowest";
        };
    }

    public String whereClause(String date) {
        return switch (this) {
            case HIGH_LOW_4W -> STR."wp.start_date > DATE_TRUNC('week', '\{date}'::date) - INTERVAL '4 week'";
            case HIGH_LOW_52W -> STR."wp.start_date > DATE_TRUNC('week', '\{date}'::date) - INTERVAL '52 week'";
            case HIGH_LOW_ALL_TIME -> "1=1";
        };
    }

    public String sequenceName() {
        return switch (this) {
            case HIGH_LOW_4W, HIGH_LOW_52W, HIGH_LOW_ALL_TIME -> "sequence_high_low";
        };
    }


}
