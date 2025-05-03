package stock.price.analytics.util;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import stock.price.analytics.model.gaps.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

public final class QueryUtil {


    public static String findTickersFVGsTaggedQueryFor(StockTimeframe timeframe, FvgType fvgType, PricePerformanceMilestone pricePerformanceMilestone, String cfdMargins) {
        String prefix = timeframe.stockPrefix();
        Pair<String, String> queryFields = switch (pricePerformanceMilestone) {
            case HIGH_52W_95, LOW_52W_95, HIGH_52W_90, LOW_52W_90 -> new MutablePair<>("low52w", "high52w");
            case HIGH_4W_95, LOW_4W_95, HIGH_4W_90, LOW_4W_90 -> new MutablePair<>("low4w", "high4w");
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95, HIGH_ALL_TIME_90, LOW_ALL_TIME_90 ->
                    new MutablePair<>("lowest", "highest");
        };
        String lowField = queryFields.getLeft();
        String highField = queryFields.getRight();
        String fvgTypeStr = fvgType.name();
        String highLowWhereClause = highLowWhereClauseFVGsTagged(pricePerformanceMilestone, lowField, highField);

        return STR."""
                SELECT fvg.*
                FROM stocks s
                JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = '\{timeframe}' and fvg.type = '\{fvgTypeStr}'
                WHERE
                s.cfd_margin in (\{cfdMargins})
                \{highLowWhereClause}
                AND (s.\{prefix}high between fvg.low AND fvg.high OR s.\{prefix}low between fvg.low AND fvg.high)
                """;
    }

    private static String highLowWhereClauseFVGsTagged(PricePerformanceMilestone pricePerformanceMilestone, String lowField, String highField) {
        String highLowWhereClause;
        double percentage = pricePerformanceMilestone.is95thPercentileValue() ? 0.95 : 0.9;
        if (PricePerformanceMilestone.highPercentileValues().contains(pricePerformanceMilestone)) {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (1 - ((s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})))) > \{percentage}";
        } else {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})) > \{percentage}";
        }
        return highLowWhereClause;
    }

    public static String checkImportStatusQueryFor(StockTimeframe timeframe, boolean checkFirstImport) {
        String timeframePeriod = timeframe.toDateTruncPeriod();
        return STR."""
                SELECT
                    COUNT(*) = \{checkFirstImport ? "0" : "1"}
                FROM
                    daily_prices
                WHERE
                    ticker = 'AAPL'
                    AND date_trunc('\{timeframePeriod}', date) = date_trunc('\{timeframePeriod}', current_date);
                """;
    }

    public static String savePriceGapsQueryFor(List<String> tickers, StockTimeframe timeframe, boolean allHistoricalData, boolean firstWeeklyImportDone) {
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String dbTable = timeframe.dbTableOHLC();
        String dateColumn = timeframe == StockTimeframe.DAILY ? "date" : "start_date";
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
                    \{dateColumn} AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY \{dateColumn} DESC) AS row_num
                FROM \{dbTable}
                WHERE ticker in (\{tickersFormatted})
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33, 0.5))
                    AND \{dateColumn} between CURRENT_DATE - INTERVAL '\{lookBackCount} \{intervalPeriod}' and (SELECT max_date from max_date_cte)
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
                    AND p2.\{dateColumn} > p1.closing_date
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

    public static String findFVGsQueryFrom(StockTimeframe timeframe, List<String> tickers, boolean allHistoricalData) {
        String date = lookbackDateFor(timeframe, allHistoricalData).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dateColumn = timeframe == StockTimeframe.DAILY ? "date" : "start_date";
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        String dbTable = timeframe.dbTableOHLC();

        return STR."""
                WITH price_data AS (
                    SELECT
                        ticker,
                        date_trunc('\{dateTruncPeriod}', \{dateColumn})::date AS wmy_date,
                        open, high, low, close,
                        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY \{dateColumn}) AS rn
                    FROM \{dbTable}
                    WHERE \{dateColumn} >= '\{date}'::date and ticker in (\{tickersFormatted})
                ),
                fvg_candidates AS (
                    SELECT a.ticker,
                        a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
                        a.high AS high1, b.high AS high2, c.high AS high3,
                        a.low AS low1, b.low AS low2, c.low AS low3
                    FROM price_data a
                    JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                    JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
                ),
                identified_fvgs AS (
                    SELECT
                        *,
                        CASE
                            WHEN low1 > high3 THEN 'BEARISH'
                            WHEN high1 < low3 THEN 'BULLISH'
                        END AS type
                    FROM fvg_candidates
                    WHERE (low1 > high3 OR high1 < low3)
                )
                SELECT
                    nextval('sequence_fvg') as id,
                    '\{timeframe}' as timeframe,
                    'OPEN' as status,
                    fvg.ticker as ticker,
                    fvg.date2 as date,
                    fvg.type,
                    CASE
                        WHEN fvg.type = 'BULLISH' THEN
                            CASE
                                WHEN low1 > high3 THEN low1
                                WHEN high1 < low3 THEN high1
                            END
                        ELSE
                            CASE
                                WHEN low1 > high3 THEN high3
                                WHEN high1 < low3 THEN low3
                            END
                    END AS LOW,
                    CASE
                        WHEN fvg.type = 'BULLISH' THEN
                            CASE
                                WHEN low1 > high3 THEN high3
                                WHEN high1 < low3 THEN low3
                            END
                        ELSE
                            CASE
                                WHEN low1 > high3 THEN low1
                                WHEN high1 < low3 THEN high1
                            END
                    END AS high,
                    null as unfilled_low1,
                    null as unfilled_high1,
                    null as unfilled_low2,
                    null as unfilled_high2
                FROM identified_fvgs fvg
                WHERE
                    NOT EXISTS (
                        SELECT 1
                        FROM price_data next_wmy
                        WHERE next_wmy.wmy_date >= fvg.date3 AND next_wmy.ticker = fvg.ticker
                          AND (
                              (fvg.type = 'BULLISH' AND next_wmy.low <= fvg.high1)
                              OR (fvg.type = 'BEARISH' AND next_wmy.high >= fvg.low1)
                          )
                    )
                order by ticker, fvg.date2 desc;
                """;
    }

    private static LocalDate lookbackDateFor(StockTimeframe timeframe, boolean allHistoricalData) {
        if (allHistoricalData) return LocalDate.of(1990, 1, 1);

        return switch (timeframe) {
            case DAILY -> LocalDate.now().minusWeeks(1);
            case WEEKLY -> LocalDate.now().minusWeeks(3);
            case MONTHLY -> LocalDate.now().minusMonths(3);
            case QUARTERLY -> LocalDate.now().minusMonths(9);
            case YEARLY -> LocalDate.now().minusYears(3);
        };
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
                    AND start_date = '\{date}'::date;
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
                    SELECT DATE_TRUNC('week', '\{date}'::date) \{allTimeHistoricalInterval} AS start_date
                ),
                cumulative_prices AS (
                    SELECT
                        wp.ticker,
                        DATE_TRUNC('week', wp.start_date) AS start_date,
                        MAX(wp.high) OVER (PARTITION BY wp.ticker ORDER BY wp.start_date ROWS BETWEEN \{intervalPreceding} PRECEDING AND CURRENT ROW) AS cumulative_high,
                        MIN(wp.low) OVER (PARTITION BY wp.ticker ORDER BY wp.start_date ROWS BETWEEN \{intervalPreceding} PRECEDING AND CURRENT ROW) AS cumulative_low
                    FROM weekly_prices wp
                    WHERE wp.ticker in (\{tickersFormatted})
                )
                INSERT INTO \{tableName} (id, high, low, start_date, end_date, ticker)
                SELECT
                    nextval('sequence_high_low') AS id,
                    cp.cumulative_high AS high,
                    cp.cumulative_low AS low,
                    date_trunc('week', wd.start_date::date)::date  AS start_date,
                    (date_trunc('week', wd.start_date::date)  + interval '4 days')::date AS end_date,
                    cp.ticker AS ticker
                FROM weekly_dates wd
                JOIN cumulative_prices cp ON cp.start_date = wd.start_date
                ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                    high = EXCLUDED.high,
                    low = EXCLUDED.low;
                """;
    }

    public static String highLowPricesNotDelistedForDateQuery(HighLowPeriod highLowPeriod) {
        return STR."""
                SELECT * FROM \{highLowPeriod.tableName()}
                WHERE start_date = :tradingDate
                AND ticker IN (SELECT ticker FROM stocks WHERE xtb_stock = true AND delisted_date IS NULL)
        """;
    }

    public static String averageCandleRangeQuery(StockTimeframe timeframe) {
        String tableName = timeframe.dbTableOHLC();
        String intervalPeriod = timeframe.toIntervalPeriod();
        String dateColumn = timeframe == StockTimeframe.DAILY ? "date" : "start_date";
        return STR."""
                SELECT ticker, AVG(high - low) AS avg_range
                FROM (
                    SELECT
                        ticker,
                        high,
                        low,
                        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY \{dateColumn} DESC) AS rn
                    FROM \{tableName}
                	where \{dateColumn} > current_date - interval '4 \{intervalPeriod}'
                ) sub
                WHERE rn <= 15
                GROUP BY ticker
                """;
    }
}
