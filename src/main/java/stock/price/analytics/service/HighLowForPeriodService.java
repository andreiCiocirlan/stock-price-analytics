package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.enums.HighLowPeriod;
import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
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
import static stock.price.analytics.model.prices.enums.HighLowPeriod.*;
import static stock.price.analytics.util.HighLowPeriodPricesUtil.highLowFromFileForPeriod;
import static stock.price.analytics.util.LoggingUtil.logElapsedTime;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {

    private static final LocalDate START_DATE = of(2022, 6, 1);
    private static final LocalDate END_DATE = of(2025, 6, 1);

    @PersistenceContext
    private final EntityManager entityManager;
    private final HighLowForPeriodRepository highLowForPeriodRepository;

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

    @Transactional
    public void saveCurrentWeekHighLowPrices(List<String> tickerList, LocalDate tradingDate) {
        logElapsedTime(() -> saveHighLowPrices(tickerList, tradingDate, false), "saved current week HighLow prices");
    }

    @Transactional
    public void saveAllHistoricalHighLowPrices(List<String> tickerList, LocalDate tradingDate) {
        logElapsedTime(() -> saveHighLowPrices(tickerList, tradingDate, true), "saved ALL Historical HighLow prices");
    }

    private void saveHighLowPrices(List<String> tickerList, LocalDate tradingDate, boolean allHistoricalPrices) {
        String tickers = tickerList.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String tradingDateFormatted = tradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            String query = queryHighLowPricesFor(highLowPeriod, tradingDateFormatted, tickers, allHistoricalPrices);
            int savedOrUpdatedCount = entityManager.createNativeQuery(query).executeUpdate();
            if (savedOrUpdatedCount != 0) {
                log.warn("saved/updated {} rows for HighLow {} weeks and date {}", savedOrUpdatedCount, intervalFrom(highLowPeriod), tradingDateFormatted);
            }
        }
    }

    private String queryHighLowPricesFor(HighLowPeriod period, String date, String tickers, boolean allHistoricalPrices) {
        String interval = intervalPrecedingFrom(period);
        String sequenceName = sequenceNameFrom(period);
        String tableName = tableNameFrom(period);
        String cumulativeWhereClause = allHistoricalPrices ? "1=1" : whereClauseFrom(period, date);
        String allTimeHistoricalInterval = allHistoricalPrices ? "- (GENERATE_SERIES(0, 3500) * INTERVAL '1 week')" : "";

        if (HIGH_LOW_ALL_TIME == period && !allHistoricalPrices)
            return queryHighestLowestPricesCurrentWeek(date, tickers, sequenceName);

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
                	MAX(cp.cumulative_high) AS high,
                    MIN(cp.cumulative_low) AS low,
                    date_trunc('week', wd.start_date::date)::date  AS start_date,
                    (date_trunc('week', wd.start_date::date)  + interval '4 days')::date AS end_date,
                    cp.ticker AS ticker
                FROM weekly_dates wd
                JOIN cumulative_prices cp ON cp.start_date = wd.start_date
                GROUP BY wd.start_date, cp.ticker
                ORDER BY cp.ticker, wd.start_date DESC
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
