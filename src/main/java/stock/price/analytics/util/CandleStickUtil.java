package stock.price.analytics.util;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;

public final class CandleStickUtil {

    public static boolean isTightCandleStick(AbstractPrice price, Double avgRange) {
        if (avgRange == null || avgRange <= 0) {
            return false;
        }
        double range = price.getHigh() - price.getLow();
        return range <= avgRange * 0.5d;
    }
}
