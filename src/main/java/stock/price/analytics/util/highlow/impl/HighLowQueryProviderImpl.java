package stock.price.analytics.util.highlow.impl;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.util.TradingDateUtil;
import stock.price.analytics.util.highlow.HighLowQueryProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HighLowQueryProviderImpl implements HighLowQueryProvider {

    @Override
    public String weeklyHighLowExistsQuery() {
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

    @Override
    public String queryAllHistoricalHighLowPricesFor(List<String> tickers, LocalDate tradingDate, HighLowPeriod highLowPeriod) {
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

    @Override
    public String highLowPricesNotDelistedForDateQuery(HighLowPeriod highLowPeriod) {
        return STR."""
                SELECT * FROM \{highLowPeriod.tableName()}
                WHERE date = :tradingDate
                AND ticker IN (SELECT ticker FROM stocks WHERE xtb_stock = true AND delisted_date IS NULL)
        """;
    }
}
