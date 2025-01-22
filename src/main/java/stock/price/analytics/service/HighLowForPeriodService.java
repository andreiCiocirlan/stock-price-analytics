package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HighLowPricesCache;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.HighLowForPeriodRepository;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static java.time.LocalDate.of;
import static stock.price.analytics.model.prices.enums.HighLowPeriod.*;
import static stock.price.analytics.util.HighLowPeriodPricesUtil.highLowFromFileForPeriod;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveNoLogging;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {

    private static final LocalDate START_DATE = of(2022, 6, 1);
    private static final LocalDate END_DATE = of(2025, 6, 1);

    @PersistenceContext
    private final EntityManager entityManager;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private final HighLowPricesCache highLowPricesCache;

    @Transactional
    public void saveHighLowPricesForPeriod(StockPerformanceInterval stockPerformanceInterval) {
        List<HighLowForPeriod> highLowForPeriod = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> {
                        try {
                            highLowForPeriod.addAll(highLowFromFileForPeriod(srcFile, START_DATE, END_DATE, stockPerformanceInterval));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<? extends PriceEntity> highLowPrices = switch (stockPerformanceInterval) {
            case STOCK_PERF_INTERVAL_30D -> highLowForPeriod.stream()
                    .map(HighLow4w.class::cast)
                    .toList();
            case STOCK_PERF_INTERVAL_52W -> highLowForPeriod.stream()
                    .map(HighLow52Week.class::cast)
                    .toList();
        };
        partitionDataAndSave(highLowPrices, highLowForPeriodRepository);
    }

    public void saveCurrentWeekHighLowPricesFrom(List<DailyPriceOHLC> dailyPricesImported, List<String> tickers) {
        for (HighLowPeriod highLowPeriod : values()) {
            List<? extends HighLowForPeriod> hlPricesUpdated = highLowPricesCache.updateHighLowPricesCacheFrom(dailyPricesImported, tickers, highLowPeriod);
            if (!hlPricesUpdated.isEmpty()) {
                log.info("found {} new {} prices {}", hlPricesUpdated.size(), highLowPeriod, hlPricesUpdated.stream().map(HighLowForPeriod::getTicker).toList());
                partitionDataAndSaveNoLogging(hlPricesUpdated, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(hlPricesUpdated, highLowPeriod);
            }
        }
    }

    public void saveCurrentWeekHighLowPricesSingleTicker(String ticker, LocalDate tradingDate) {
        saveCurrentWeekHighLowPrices(List.of(ticker), tradingDate);
    }

    public void saveCurrentWeekHighLowPrices(List<String> tickers, LocalDate tradingDate) {
        logTime(() -> saveHighLowPrices(tickers, tradingDate, false), "saved current week HighLow prices");
    }

    public void saveAllHistoricalHighLowPrices(List<String> tickers, LocalDate tradingDate) {
        logTime(() -> saveHighLowPrices(tickers, tradingDate, true), "saved ALL historical HighLow prices");
    }

    public void saveAllHistoricalHighLowPricesSingleTicker(String ticker, LocalDate tradingDate) {
        saveAllHistoricalHighLowPrices(List.of(ticker), tradingDate);
    }

    private void saveHighLowPrices(List<String> tickers, LocalDate tradingDate, boolean allHistoricalPrices) {
        for (HighLowPeriod highLowPeriod : values()) {
            String msg = String.format("saved %d %s rows for %s", tickers.size(), highLowPeriod.tableName(), tradingDate);
            logTime(() -> executeQueryHLPricesForPeriod(tickers, tradingDate, allHistoricalPrices, highLowPeriod), msg);
        }
    }

    private void executeQueryHLPricesForPeriod(List<String> tickers, LocalDate tradingDate, boolean allHistoricalPrices, HighLowPeriod highLowPeriod) {
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String query = queryHighLowPricesFor(highLowPeriod, tradingDate, tickersFormatted, allHistoricalPrices);
        entityManager.createNativeQuery(query).executeUpdate();
    }

    private String queryHighLowPricesFor(HighLowPeriod period, LocalDate tradingDate, String tickers, boolean allHistoricalPrices) {
        String date = tradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sequenceName = period.sequenceName();
        String tableName = period.tableName();
        String interval = period.intervalPreceding();

        if (!allHistoricalPrices) {
            if (HIGH_LOW_ALL_TIME == period) {
                return queryHighestLowestPricesCurrentWeek(date, tickers, sequenceName);
            }
            if (HIGH_LOW_52W == period || HIGH_LOW_4W == period) {
                // monday to prevent using date_trunc which is overhead for the query
                String monday = tradingDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return queryHighLow52wOr4wPricesCurrentWeek(tableName, interval, monday, tickers, sequenceName);
            }
        }
        String cumulativeWhereClause = allHistoricalPrices ? "1=1" : period.whereClause(date);
        String allTimeHistoricalInterval = allHistoricalPrices ? "- (GENERATE_SERIES(0, 3500) * INTERVAL '1 week')" : "";

        // entire historical prices update
        return STR."""
                WITH weekly_dates AS (
                	SELECT DATE_TRUNC('week', '\{date}'::date) \{allTimeHistoricalInterval} AS start_date
                ),
                cumulative_prices AS (
                    SELECT
                        wp.ticker,
                        DATE_TRUNC('week', wp.start_date) AS start_date,
                        MAX(wp.high) OVER (PARTITION BY wp.ticker ORDER BY wp.start_date ROWS BETWEEN \{interval} PRECEDING AND CURRENT ROW) AS cumulative_high,
                        MIN(wp.low) OVER (PARTITION BY wp.ticker ORDER BY wp.start_date ROWS BETWEEN \{interval} PRECEDING AND CURRENT ROW) AS cumulative_low
                    FROM weekly_prices wp
                	WHERE
                		\{cumulativeWhereClause} and wp.ticker in (\{tickers})
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
    }

    private String queryHighLow52wOr4wPricesCurrentWeek(String tableName, String interval, String date, String tickers, String sequenceName) {
        return STR."""
                WITH hl_for_period AS (
                	SELECT
                		wp.ticker,
                		MAX(wp.high) as high,
                		MIN(wp.low) as low
                	FROM weekly_prices wp
                	WHERE wp.start_date >= '\{date}'::date - INTERVAL '\{interval} week'
                	    and ticker not in (select ticker from stocks where delisted_date is not null)
                	    and ticker in (\{tickers})
                	group by wp.ticker
                )
                INSERT INTO \{tableName} (id, high, low, start_date, end_date, ticker)
                SELECT
                	nextval('\{sequenceName}') AS id,
                	hlp.high,
                    hlp.low,
                    date_trunc('week', '\{date}'::date)::date  AS start_date,
                    (date_trunc('week', '\{date}'::date)  + interval '4 days')::date AS end_date,
                    hlp.ticker
                FROM hl_for_period hlp
                ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                	high = EXCLUDED.high,
                	low = EXCLUDED.low;
                """;
    }

    private String queryHighestLowestPricesCurrentWeek(String date, String tickers, String sequenceName) {
        return STR."""
                WITH hl_data AS (
                    SELECT
                        hlp.ticker,
                        MAX(hlp.high) AS highest,
                        MIN(hlp.low) AS lowest
                    FROM highest_lowest hlp
                    WHERE
                        hlp.start_date BETWEEN (DATE_TRUNC('week', '\{date}'::date) - INTERVAL '1 week')
                        AND '\{date}'::date
                        AND hlp.ticker IN (\{tickers})
                    GROUP BY hlp.ticker
                )
                INSERT INTO public.highest_lowest (id, high, low, start_date, end_date, ticker)
                SELECT
                	nextval('\{sequenceName}') AS id,
                    GREATEST(wp.high, hl.highest),
                    LEAST(wp.low, hl.lowest),
                    DATE_TRUNC('week', wp.start_date)::date,
                    (DATE_TRUNC('week', wp.start_date) + INTERVAL '4 days')::date,
                    wp.ticker
                FROM weekly_prices_performance_view wp
                LEFT JOIN hl_data hl ON hl.ticker = wp.ticker
                WHERE wp.ticker IN (\{tickers})
                AND DATE_TRUNC('week', wp.start_date) = DATE_TRUNC('week', '\{date}'::date)
                ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                	high = EXCLUDED.high,
                	low = EXCLUDED.low
                """;
    }
}
