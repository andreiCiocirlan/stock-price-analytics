package stock.price.analytics.util;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public final class Constants {

    public static final String USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0";
    public static final ZoneId NY_ZONE = ZoneId.of("America/New_York");
    public static final LocalTime START_MARKET_HOURS_NYSE = LocalTime.of(9, 30);
    public static final LocalTime END_MARKET_HOURS_NYSE = LocalTime.of(16, 0);
    public static final int MAX_TICKER_COUNT_PRINT = 100;
    public static final String FVG_95TH_PERCENTILE_4W = "95% 4w";
    public static final String FVG_95TH_PERCENTILE_52W = "95% 52w";
    public static final String FVG_95TH_PERCENTILE_ALL_TIME = "95% All-Time";
    public static final String FVG_90TH_PERCENTILE_4W = "90% 4w";
    public static final String FVG_90TH_PERCENTILE_52W = "90% 52w";
    public static final String FVG_90TH_PERCENTILE_ALL_TIME = "90% All-Time";
    public static final List<Double> CFD_MARGINS_5X = List.of(0.2);
    public static final List<Double> CFD_MARGINS_5X_4X = List.of(0.2, 0.25);
    public static final List<Double> CFD_MARGINS_5X_4X_3X = List.of(0.2, 0.25, 0.33);
    public static final List<Double> CFD_MARGINS_5X_4X_3X_2X = List.of(0.2, 0.25, 0.33, 0.5);
    public static final List<Double> CFD_MARGINS_5X_4X_3X_2X_1X = List.of(0.2, 0.25, 0.33, 0.5, 0.0);
    public static final Double MIN_GAP_AND_GO_PERCENTAGE = 0.04d;
    public static final Double INTRADAY_SPIKE_PERCENTAGE = 0.02d; // percentage of price movement between imports (20m) used for alerting spikes
    public static final String HTTP_LOCALHOST = "http://localhost:";
    public static final String YAHOO_QUOTES_IMPORT_ENDPOINT = "/yahoo-quotes/import";
}
