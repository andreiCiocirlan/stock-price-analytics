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
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.HighLowForPeriodRepository;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static java.time.LocalDate.of;
import static stock.price.analytics.model.prices.enums.HighLowPeriod.values;
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
                    .parallel()
                    .forEachOrdered(srcFile -> highLowForPeriod.addAll(highLowFromFileForPeriod(srcFile, START_DATE, END_DATE, stockPerformanceInterval)));
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

    public void saveCurrentWeekHighLowPricesFrom(List<DailyPrice> dailyPricesImported, List<String> tickers) {
        for (HighLowPeriod highLowPeriod : values()) {
            List<? extends HighLowForPeriod> hlPricesUpdated = highLowPricesCache.updateHighLowPricesCacheFrom(dailyPricesImported, tickers, highLowPeriod);
            if (!hlPricesUpdated.isEmpty()) {
                log.info("found {} new {} prices {}", hlPricesUpdated.size(), highLowPeriod, hlPricesUpdated.stream().map(HighLowForPeriod::getTicker).toList());
                partitionDataAndSaveNoLogging(hlPricesUpdated, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(hlPricesUpdated, highLowPeriod);
            }
        }
    }

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

}
