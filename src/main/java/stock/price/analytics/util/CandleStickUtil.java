package stock.price.analytics.util;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;

public final class CandleStickUtil {

    public static boolean isTightCandleStick(AbstractPrice price, Double avg15DaysRange) {
        if (avg15DaysRange == null || avg15DaysRange <= 0) {
            throw new IllegalArgumentException("Average range must be a positive number");
        }
        double range = price.getHigh() - price.getLow();
        return range <= avg15DaysRange * 0.5d;
    }
}
