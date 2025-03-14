package stock.price.analytics.util;

import java.time.LocalDate;
import java.util.List;

public class Constants {

    public static final String STOCKS_LOCATION = "D:\\stocks-data\\historical_prices\\ALL_STOCKS\\";
    public static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";
    public static final String CLASSPATH_TICKERS_XTB_TXT = "classpath:tickers_XTB.txt";
    public static final String TIMEFRAME_PATTERN = "^(DAILY|WEEKLY|MONTHLY|YEARLY)$";
    public static final String HIGHER_TIMEFRAMES_PATTERN = "^[WMQYwmqy]{1,4}$";
    public static final int MAX_TICKER_COUNT_PRINT = 100;
    public static final String FVG_95TH_PERCENTILE_4W = "95% 4w";
    public static final String FVG_95TH_PERCENTILE_52W = "95% 52w";
    public static final String FVG_95TH_PERCENTILE_ALL_TIME = "95% All-Time";
    public static final LocalDate DAILY_FVG_MIN_DATE = LocalDate.of(2022, 1, 31);
    public static final List<Double> CFD_MARGINS_5X = List.of(0.2);
    public static final List<Double> CFD_MARGINS_5X_4X = List.of(0.2, 0.25);
    public static final List<Double> CFD_MARGINS_5X_4X_3X = List.of(0.2, 0.25, 0.33);
    public static final List<Double> CFD_MARGINS_5X_4X_3X_2X = List.of(0.2, 0.25, 0.33, 0.5);
    public static final Double MIN_GAP_AND_GO_PERCENTAGE = 0.04d;
    public static final Double INTRADAY_SPIKE_PERCENTAGE = 0.02d; // percentage of price movement between imports (20m) used for alerting spikes
}
