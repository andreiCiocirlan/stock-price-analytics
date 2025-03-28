package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.highlow.enums.HighLowPeriod.values;
import static stock.price.analytics.util.Constants.NY_ZONE;
import static stock.price.analytics.util.LoggingUtil.logTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;

    @Transactional
    public void saveCurrentWeekHighLowPricesFrom(List<DailyPrice> dailyPrices) {
        List<String> tickers = dailyPrices.stream().map(DailyPrice::getTicker).toList();
        for (HighLowPeriod highLowPeriod : values()) {
            List<? extends HighLowForPeriod> hlPricesUpdated = cacheService.getUpdatedHighLowPricesForTickers(dailyPrices, tickers, highLowPeriod);
            if (!hlPricesUpdated.isEmpty()) {
                asyncPersistenceService.partitionDataAndSaveNoLogging(hlPricesUpdated, highLowForPeriodRepository);
                cacheService.addHighLowPrices(hlPricesUpdated, highLowPeriod);
            }
        }
    }

    @Transactional
    public void saveAllHistoricalHighLowPrices(List<String> tickers, LocalDate tradingDate) {
        logTime(() -> executeQueryAllHistoricalHLPrices(tickers, tradingDate), "saved ALL historical HighLow prices for " + tickers);
    }

    private void executeQueryAllHistoricalHLPrices(List<String> tickers, LocalDate tradingDate) {
        for (HighLowPeriod highLowPeriod : values()) {
            String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
            String date = tradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String sequenceName = highLowPeriod.sequenceName();
            String tableName = highLowPeriod.tableName();
            String intervalPreceding = highLowPeriod.intervalPreceding();

            String allTimeHistoricalInterval = "- (GENERATE_SERIES(0, 3500) * INTERVAL '1 week')";

            // entire historical prices update
            String query = STR."""
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
                        nextval('\{sequenceName}') AS id,
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
            int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
            log.info("saved {} rows for {} and high low period {}", rowsAffected, tickers, highLowPeriod);
        }
    }

    public boolean weeklyHighLowExists() {
        String date = LocalDate.now(NY_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String query = STR."""
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

        Query nativeQuery = entityManager.createNativeQuery(query, Boolean.class);

        return (Boolean) nativeQuery.getResultList().getFirst();
    }

}
