package stock.price.analytics.controller.dto;


import java.time.LocalDate;

public record CandleOHLCWithDate(LocalDate date, double open, double high, double low, double close) {

    public CandleOHLCWithDate {
        // Validate that high is greater than low
        if (high < low) {
            if (Math.abs(high) <= 0.11 * low && Math.abs(high) >= 0.09 * low) { // correct high is 10% of actual high price
                high = 10 * high;
            } else if (Math.abs(high) <= low * 1.03 && Math.abs(high) >= 0.97 * low) { // correct high and low prices being mistakenly reversed
                double temp = high;
                high = low;
                low = temp;
            } else // way off the mark (manual error)
                throw new NumberFormatException(" High price: " + high + " must be greater than low price: " + low);
        }

        // Validate that open and close prices are between high and low
        if (open < low || open > high) {
            // correct missing opening price (equal to close price should not affect much)
            // if it's 10% of actual open price (between 0.1 * [low <--> high] )
            if (open == 0 || (Math.abs(open) >= 0.1 * low && Math.abs(open) <= 0.1 * high)) {
                open = close;
            } else if (open < high * 0.9 || open > high * 1.1) { // way off the mark (manual error)
                throw new IllegalArgumentException(open + " must be between: " + low + " and " + high);
            }
        }

        if (close < low || close > high) { // if still not within the range after correction throw exception
            // if it's 10% of actual close price (between 0.1 * [low <--> high] )
            if (Math.abs(close) >= 0.10 * low && Math.abs(close) <= 0.10 * high) {
                close = 10 * close;
            } else if (close < high * 0.9 || close > high * 1.1) { // way off the mark (manual error)
                throw new IllegalArgumentException("Closing price " + close + "  must be between " + high + " high and low " + low);
            }
        }

        // Validate numeric validity (optional)
        if (Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close) ||
                open < 0 || high < 0 || low < 0 || close < 0) {
            throw new IllegalArgumentException("All prices must be valid non-negative numeric values.");
        }
    }

}