package stock.price.analytics.config;

import java.time.LocalTime;

public class MarketHours {

    private static final LocalTime startMarketHours = LocalTime.of(16, 30, 0, 0);
    private static final LocalTime endMarketHours = LocalTime.of(23, 0, 0, 0);

    public static boolean isBetweenMarketHours(LocalTime currentTime) {
        return currentTime.isAfter(startMarketHours) && currentTime.isBefore(endMarketHours);
    }

}
