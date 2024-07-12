package stock.price.analytics.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockWithPrevCloseDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
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
    private final RefreshMaterializedViewsService refreshMaterializedViewsService;

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
            case DAILY -> priceOHLCS.stream().map(DailyPriceOHLC.class::cast).toList();
            case WEEKLY -> priceOHLCS.stream().map(WeeklyPriceOHLC.class::cast).toList();
            case MONTHLY -> priceOHLCS.stream().map(MonthlyPriceOHLC.class::cast).toList();
            case YEARLY -> priceOHLCS.stream().map(YearlyPriceOHLC.class::cast).toList();
        };
    }

    @Transactional
    public void savePricesAfterTradingDate(String tickers, int prevDaysCount, LocalDate tradingDate) {
        List<DailyPriceOHLC> prevDaysHistPrices = new ArrayList<>();
        Map<String, List<DailyPriceOHLC>> tickerAndPrevDaysPricesImported = new HashMap<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                    String stockTicker = tickerFrom(srcFile);
                    if (tickers != null && Arrays.stream(tickers.split(",")).noneMatch(ticker -> ticker.equals(stockTicker)))
                        return;
                    List<DailyPriceOHLC> dailyPriceOHLCS = dailyPricesFromFileLastDays(srcFile, prevDaysCount + 1); // +1 to get previous day close
                    if (dailyPriceOHLCS.getLast().getDate().isAfter(tradingDate.minusDays(1))) {
                        tickerAndPrevDaysPricesImported.put(stockTicker, dailyPriceOHLCS);
                    } else {
                        log.info("Didn't save daily prices for ticker {} and date {}", stockTicker, dailyPriceOHLCS.getLast().getDate());
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        tickerAndPrevDaysPricesImported.forEach((_, dailyPricesPrevDays) -> prevDaysHistPrices.add(latestDailyPricesWithPerformance(dailyPricesPrevDays, dailyPricesPrevDays.getFirst(), dailyPricesPrevDays.getLast())));

        partitionDataAndSave(prevDaysHistPrices, priceOhlcRepository);

        // insert/update higher timeframe prices
        updateHigherTimeframesPricesFor(tradingDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // finally refresh all the materialized views that use the above data
        refreshMaterializedViewsService.refreshMaterializedViews();
    }

    // may be used to provide price performance between latest trading date and N days in the past (depends on dailyPrices size)
    private DailyPriceOHLC latestDailyPricesWithPerformance(List<DailyPriceOHLC> dailyPrices, DailyPriceOHLC dailyOHLCInThePast, DailyPriceOHLC latestOHLCPrices) {
        if (dailyPrices.size() < 2) { // IPO (dailyOHLCInThePast = latestOHLCPrices)
            latestOHLCPrices.setPerformance(0);
        } else {
            double previousClose = dailyOHLCInThePast.getClose();
            double performance = ((latestOHLCPrices.getClose() - previousClose) / previousClose) * 100;
            latestOHLCPrices.setPerformance(Math.round(performance * 100.0) / 100.0);
        }
        return latestOHLCPrices;
    }

    private Map<String, StockWithPrevCloseDTO> tickerAndPrevCloseFor(int prevDaysCount) {
        String queryStr = STR."""
            SELECT ticker, date, close as prev_close
            FROM (SELECT ticker, date, close, ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS rn FROM daily_prices) AS subquery
            WHERE rn = \{prevDaysCount}
        """;

        Query nativeQuery = entityManager.createNativeQuery(queryStr, StockWithPrevCloseDTO.class);

        @SuppressWarnings("unchecked")
        List<StockWithPrevCloseDTO> prevDayTickerAndClosingPrices = (List<StockWithPrevCloseDTO>) nativeQuery.getResultList();

        return prevDayTickerAndClosingPrices.stream().collect(Collectors.toMap(
                StockWithPrevCloseDTO::ticker,
                stock -> new StockWithPrevCloseDTO(stock.ticker(), stock.date(), stock.prevClose())
        ));
    }

    private void updateHigherTimeframesPricesFor(String dateFormatted) {
        updateHigherTimeframeHistPrices("week", "weekly_prices", dateFormatted);
        updateHigherTimeframeHistPrices("month", "monthly_prices", dateFormatted);
        updateHigherTimeframeHistPrices("year", "yearly_prices", dateFormatted);
    }

    @Transactional
    public void saveAllDailyPricesFromFiles() {
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
    public void updateHigherTimeframeHistPrices(String timeframe, String tableName, String date) {
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
        if (savedOrUpdatedCount != 0) {
            log.info("saved/updated {} {} rows for date {}", savedOrUpdatedCount, timeframe, date);
        }
    }

    @Transactional
    public void saveHigherTimeframePricesBetween(LocalDate startDate, LocalDate endDate) {
        while (startDate.isBefore(endDate)) {
            String dateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            updateHigherTimeframesPricesFor(dateStr);
            startDate = startDate.plusDays(7);
        }

        refreshMaterializedViewsService.refreshMaterializedViews();
    }

}