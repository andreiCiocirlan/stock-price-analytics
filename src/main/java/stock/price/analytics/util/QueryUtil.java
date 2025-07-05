package stock.price.analytics.util;

import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

public final class QueryUtil {


    public static String savePriceGapsQueryFor(List<String> tickers, StockTimeframe timeframe, boolean allHistoricalData, boolean firstWeeklyImportDone) {
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String dbTable = timeframe.dbTableOHLC();
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        String interval = timeframe.toInterval();
        String intervalPeriod = timeframe.toIntervalPeriod();
        int lookBackCount = timeframe == StockTimeframe.DAILY && firstWeeklyImportDone ? 3 : 1;
        if (allHistoricalData) {
            lookBackCount = switch (timeframe) {
                case DAILY -> 1000;
                case WEEKLY -> 300;
                case MONTHLY -> 200;
                case QUARTERLY -> 100;
                case YEARLY -> 10;
            };
        }

        return STR."""
            WITH max_date_cte AS (
                select date_trunc('\{dateTruncPeriod}', (select max(last_updated) from stocks)) - INTERVAL '\{interval}' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS row_num
                FROM \{dbTable}
                WHERE ticker in (\{tickersFormatted})
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33, 0.5))
                    AND date between CURRENT_DATE - INTERVAL '\{lookBackCount} \{intervalPeriod}' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= \{lookBackCount} and NOT EXISTS (
                    SELECT 1
                    FROM \{dbTable} p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.date > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'\{timeframe}',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """;
    }

    public static String weeklyHighLowExistsQuery() {
        String date = TradingDateUtil.dateNowInNY().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(DateTimeFormatter.ISO_LOCAL_DATE);
        return STR."""
                SELECT
                    CASE
                        WHEN COUNT(*) = 1 THEN TRUE
                        ELSE FALSE
                    END AS result
                FROM
                    high_low4w
                WHERE
                    ticker = 'AAPL'
                    AND date = '\{date}'::date;
                """;
    }

    public static String queryAllHistoricalHighLowPricesFor(List<String> tickers, LocalDate tradingDate, HighLowPeriod highLowPeriod) {
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String date = tradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String tableName = highLowPeriod.tableName();
        String intervalPreceding = highLowPeriod.intervalPreceding();
        String allTimeHistoricalInterval = "- (GENERATE_SERIES(0, 3500) * INTERVAL '1 week')";

        return STR."""
                WITH weekly_dates AS (
                    SELECT DATE_TRUNC('week', '\{date}'::date) \{allTimeHistoricalInterval} AS date
                ),
                cumulative_prices AS (
                    SELECT
                        wp.ticker,
                        DATE_TRUNC('week', wp.date) AS date,
                        MAX(wp.high) OVER (PARTITION BY wp.ticker ORDER BY wp.date ROWS BETWEEN \{intervalPreceding} PRECEDING AND CURRENT ROW) AS cumulative_high,
                        MIN(wp.low) OVER (PARTITION BY wp.ticker ORDER BY wp.date ROWS BETWEEN \{intervalPreceding} PRECEDING AND CURRENT ROW) AS cumulative_low
                    FROM weekly_prices wp
                    WHERE wp.ticker in (\{tickersFormatted})
                )
                INSERT INTO \{tableName} (id, high, low, date, ticker)
                SELECT
                    nextval('sequence_high_low') AS id,
                    cp.cumulative_high AS high,
                    cp.cumulative_low AS low,
                    date_trunc('week', wd.date::date)::date  AS date,
                    cp.ticker AS ticker
                FROM weekly_dates wd
                JOIN cumulative_prices cp ON cp.date = wd.date
                ON CONFLICT (ticker, date)
                DO UPDATE SET
                    high = EXCLUDED.high,
                    low = EXCLUDED.low;
                """;
    }

    public static String highLowPricesNotDelistedForDateQuery(HighLowPeriod highLowPeriod) {
        return STR."""
                SELECT * FROM \{highLowPeriod.tableName()}
                WHERE date = :tradingDate
                AND ticker IN (SELECT ticker FROM stocks WHERE xtb_stock = true AND delisted_date IS NULL)
        """;
    }

    public static String updateStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe) {
        String prefix = timeframe.stockPrefix();
        String dbTable = timeframe.dbTableOHLC();
        String intervalPeriod = timeframe.toIntervalPeriod();
        return STR."""
                WITH discrepancies AS (
                SELECT s.ticker, p.open
                FROM public.stocks s
                JOIN \{dbTable} p ON p.date = DATE_TRUNC('\{intervalPeriod}', s.last_updated) AND p.ticker = s.ticker
                WHERE s.delisted_date IS NULL AND (p.open <> s.\{prefix}open)
                )
                UPDATE stocks s SET \{prefix}open = dscr.open
                FROM discrepancies dscr
                WHERE s.ticker = dscr.ticker;
                """;
    }
}
