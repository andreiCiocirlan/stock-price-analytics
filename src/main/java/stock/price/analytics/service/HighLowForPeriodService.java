package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.PriceEntity;
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
import static stock.price.analytics.util.HighLowPeriodPricesUtil.highLowFromFileForPeriod;
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
    public void updateHighLow(List<String> tickerList, LocalDate tradingDate) {
        String tickers = tickerList.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String tradingDateFormatted = tradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        updateHighLowForPeriod(StockPerformanceInterval.STOCK_PERF_INTERVAL_30D, tickers, tradingDateFormatted);
        updateHighLowForPeriod(StockPerformanceInterval.STOCK_PERF_INTERVAL_52W, tickers, tradingDateFormatted);
    }

    public void updateHighLowForPeriod(StockPerformanceInterval period, String tickers, String tradingDate) {
        String interval = StockPerformanceInterval.STOCK_PERF_INTERVAL_30D == period ? "4 weeks" : "52 weeks";
        String tableName = StockPerformanceInterval.STOCK_PERF_INTERVAL_30D == period ? "high_low4w" : "high_low52w";

        updateHighLowPricesForInterval(tradingDate, tableName, interval, tickers);
    }

    private void updateHighLowPricesForInterval(String date, String tableName, String interval, String tickers) {
        String query = queryHighLowPricesForInterval(date, tableName, interval, tickers);
        int savedOrUpdatedCount = entityManager.createNativeQuery(query).executeUpdate();
        if (savedOrUpdatedCount != 0) {
            log.warn("saved/updated {} {} rows for date {}", savedOrUpdatedCount, interval, date);
        }
    }

    private String queryHighLowPricesForInterval(String date, String tableName, String interval, String tickers) {
        return STR."""
                INSERT INTO \{tableName} (id, high, low, weekly_close, start_date, end_date, ticker)
                SELECT
                    nextval('sequence_high_low') AS id,
                	max(wp.high),
                	min(wp.low),
                	current_week.close,
                    date_trunc('week', '\{date}'::date)::date  AS start_date,
                    (date_trunc('week', '\{date}'::date)  + interval '4 days')::date AS end_date,
                	wp.ticker
                FROM weekly_prices wp
                LEFT JOIN (
                    SELECT ticker, close
                    FROM weekly_prices
                    WHERE start_date >= date_trunc('week', '\{date}'::date)
                        AND start_date < date_trunc('week', '\{date}'::date) + INTERVAL '1 week'
                ) current_week ON wp.ticker = current_week.ticker
                WHERE
                	wp.ticker in (\{tickers}) and
                	wp.start_date between (date_trunc('week', '\{date}'::date) - INTERVAL '\{interval}') and '\{date}'::date
                GROUP BY wp.ticker, current_week.close
                ORDER BY wp.ticker
                ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                	high = EXCLUDED.high,
                	low = EXCLUDED.low,
                	weekly_close = EXCLUDED.weekly_close;
                """;
    }
}
