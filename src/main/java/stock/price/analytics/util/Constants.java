package stock.price.analytics.util;

public class Constants {

    public static final String STOCKS_LOCATION = "D:\\stocks-data\\historical_prices\\ALL_STOCKS\\";
    public static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";
    public static final String CLASSPATH_TICKERS_XTB_TXT = "classpath:tickers_XTB.txt";
    public static final String TIMEFRAME_PATTERN = "^(DAILY|WEEKLY|MONTHLY|YEARLY)$";
    public static final String HIGHER_TIMEFRAMES_PATTERN = "^[WMQYwmqy]{1,4}$";
    public static final int MAX_TICKER_COUNT_PRINT = 100;
}
