package stock.price.analytics.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.materializedviews.RefreshMaterializedViewsRepository;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PricesOHLCUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoricalPricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PriceOHLCRepository priceOhlcRepository;
    private final RefreshMaterializedViewsRepository refreshMaterializedViewsRepository;

    private static List<? extends AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) {
        List<AbstractPriceOHLC> priceOHLCS = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        priceOHLCS.addAll(pricesOHLCForFileAndTimeframe(srcFile, stockTimeframe));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return switch (stockTimeframe) {
            case WEEKLY -> priceOHLCS.stream().map(WeeklyPriceOHLC.class::cast).toList();
            case MONTHLY -> priceOHLCS.stream().map(MonthlyPriceOHLC.class::cast).toList();
            case YEARLY -> priceOHLCS.stream().map(YearlyPriceOHLC.class::cast).toList();
        };
    }

    @Transactional
    public void saveLastWeekPricesFromFiles(String tickers) {
        List<DailyPriceOHLC> lastWeekDailyPrices = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        if (tickers != null && Arrays.stream(tickers.split(",")).noneMatch(ticker -> ticker.equals(tickerFrom(srcFile))))
                            return;
                        List<DailyPriceOHLC> dailyPriceOHLCS = dailyPricesFromFileLastWeek(srcFile, 5);
                        lastWeekDailyPrices.addAll(dailyPriceOHLCS); // some files might end with empty line -> 7 for good measure instead of 5
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // filter out spillover data from previous weeks, or from files that end in empty line
        LocalDate lastSunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        List<DailyPriceOHLC> lastWeekDailyPricesFinal = lastWeekDailyPrices.stream().filter(dp -> dp.getDate().isAfter(lastSunday)).toList();

//        partitionDataAndSave(lastWeekDailyPricesFinal, pricesOhlcRepository);

        String dateFormat = "yyyy-MM-dd";
        LocalDate localDate = LocalDate.of(2024, 5, 4);
        while (localDate.isBefore(LocalDate.now())) {
            String dateStr = localDate.format(DateTimeFormatter.ofPattern(dateFormat));
//            insertOrUpdatePrices("month", "monthly_prices", dateStr);
            insertOrUpdatePrices("year", "yearly_prices", dateStr);
            localDate = localDate.plusDays(7);
        }

//        refreshMaterializedViews();
    }

    @Transactional
    public void saveDailyPricesFromFiles() {
        List<AbstractPriceOHLC> ohlcList = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        ohlcList.addAll(dailyPricesOHLCFromFile(srcFile));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<DailyPriceOHLC> dailyOHLCList = ohlcList.stream()
                .map(DailyPriceOHLC.class::cast)
                .toList();
        log.info("OPEN_IS_ZERO_ERROR {} problems", OPEN_IS_ZERO_ERROR.get());
        log.info("HIGH_LOW_ERROR {} problems", HIGH_LOW_ERROR.get());
        log.info("ohlcList size {}", dailyOHLCList.size());
        partitionDataAndSave(dailyOHLCList, priceOhlcRepository);
    }

    @Transactional
    public void savePricesForTimeframe(StockTimeframe stockTimeframe) {
        List<? extends AbstractPriceOHLC> pricesOHLCs = pricesOHLCForTimeframe(stockTimeframe);
        partitionDataAndSave(pricesOHLCs, priceOhlcRepository);
    }

    @Transactional
    public void insertOrUpdatePrices(String timeframe, String tableName, String date) {
        String query = STR."""
            WITH interval_data AS (
            SELECT
                ticker,
                MIN(date) AS start_date,
                MAX(date) AS end_date,
                MAX(high) AS high,
                MIN(low) AS low
            FROM daily_prices
            WHERE date BETWEEN '\{date}'::date - INTERVAL '2 \{timeframe}' AND '\{date}'
            GROUP BY ticker, DATE_TRUNC('\{timeframe}', date)
            ),
            last_week AS (
                SELECT
                    dp_o.ticker,
                    start_date,
                    end_date,
                    interval_data.high,
                    interval_data.low,
                    dp_o.open,
                    dp_c.close,
                    CASE
                        WHEN (dp_o.date >= DATE_TRUNC('\{timeframe}', CURRENT_DATE)) THEN
                                    ROUND((100.0 * (dp_c.close - dp_o.open) / dp_o.open)::numeric, 2)
                        WHEN (LAG(dp_c.close, 1) OVER (PARTITION BY dp_o.ticker ORDER BY start_date) IS NULL) THEN
                            CASE
                                WHEN (dp_o.open <> 0) THEN ROUND((100.0 * (dp_c.close - dp_o.open) / dp_o.open)::numeric, 2)
                                ELSE NULL
                            END
                        ELSE
                            ROUND(((dp_c.close - LAG(dp_c.close) OVER (PARTITION BY dp_o.ticker ORDER BY start_date)) / LAG(dp_c.close) OVER (PARTITION BY dp_o.ticker ORDER BY start_date) * 100)::numeric, 2)
                    END AS performance
                FROM interval_data
                JOIN daily_prices dp_o ON dp_o.ticker = interval_data.ticker AND dp_o.date = interval_data.start_date
                JOIN daily_prices dp_c ON dp_c.ticker = interval_data.ticker AND dp_c.date = interval_data.end_date
            ),
            final_result AS (
                SELECT *
                FROM last_week
                    WHERE end_date BETWEEN DATE_TRUNC('week', '\{date}'::date) AND '\{date}' -- 'week' to capture EOY/EOM where 31st on Mon/Tue/Wed/Thu
            )
            INSERT INTO \{tableName} (ticker, start_date, end_date, high, low, open, close, performance)
            SELECT ticker, start_date, end_date, high, low, open, close, performance
            FROM final_result
            ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                    high = EXCLUDED.high,
                    low = EXCLUDED.low,
                    close = EXCLUDED.close,
                    performance = EXCLUDED.performance,
                    end_date = EXCLUDED.end_date
            """;

        int savedOrUpdatedCount = entityManager.createNativeQuery(
                String.format(query, timeframe, tableName)
        ).executeUpdate();
        log.info("saved {} {} rows for date {}", savedOrUpdatedCount, timeframe,date);
    }

    @Transactional
    public void saveWeeklyMonthlyYearlyPricesAfterDate(LocalDate startDate) {
        String dateFormat = "yyyy-MM-dd";
        while (startDate.isBefore(LocalDate.now())) {
            String dateStr = startDate.format(DateTimeFormatter.ofPattern(dateFormat));
            insertOrUpdatePrices("week", "weekly_prices", dateStr);
            insertOrUpdatePrices("month", "monthly_prices", dateStr);
            insertOrUpdatePrices("year", "yearly_prices", dateStr);
            startDate = startDate.plusDays(7);
        }

        refreshMaterializedViews();
    }

    private void refreshMaterializedViews() {
        refreshMaterializedViewsRepository.refreshWeeklyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshMonthlyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshYearlyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshHighLow4w();
        refreshMaterializedViewsRepository.refreshHighLow52w();
    }

}