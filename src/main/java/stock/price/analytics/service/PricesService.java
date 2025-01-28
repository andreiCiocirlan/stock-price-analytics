package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HigherTimeframePricesCache;
import stock.price.analytics.controller.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.PricesRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.*;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveNoLogging;
import static stock.price.analytics.util.StockDateUtils.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class PricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PricesRepository pricesRepository;
    private final HigherTimeframePricesCache higherTimeframePricesCache;

    public List<CandleWithDateDTO> findFor(String ticker, StockTimeframe timeframe) {
        String tableNameOHLC = timeframe.dbTableOHLC();
        String orderByIdField = timeframe == DAILY ? "date" : "start_date";
        String queryStr = STR."SELECT \{orderByIdField}, open, high, low, close FROM \{tableNameOHLC} WHERE ticker = :ticker ORDER BY \{orderByIdField} ASC";

        Query nativeQuery = entityManager.createNativeQuery(queryStr, CandleWithDateDTO.class);
        nativeQuery.setParameter("ticker", ticker);

        @SuppressWarnings("unchecked")
        List<CandleWithDateDTO> candles = (List<CandleWithDateDTO>) nativeQuery.getResultList();

        return candles;
    }

    public List<AbstractPriceOHLC> updatePricesForHigherTimeframes(List<DailyPriceOHLC> importedDailyPrices) {
        return logTimeAndReturn(() -> updateHTF(importedDailyPrices), "updated prices for higher timeframes");
    }

    private List<AbstractPriceOHLC> updateHTF(List<DailyPriceOHLC> importedDailyPrices) {
        List<String> tickers = new ArrayList<>(importedDailyPrices.stream().map(DailyPriceOHLC::getTicker).toList());

        // Fetch previous prices for each timeframe
        List<WeeklyPriceOHLC> previousTwoWeeklyPrices = getPreviousTwoWeeklyPrices(tickers);
        List<MonthlyPriceOHLC> previousTwoMonthlyPrices = getPreviousTwoMonthlyPrices(tickers);
        List<QuarterlyPriceOHLC> previousTwoQuarterlyPrices = getPreviousTwoQuarterlyPrices(tickers);
        List<YearlyPriceOHLC> previousTwoYearlyPrices = getPreviousTwoYearlyPrices(tickers);

        // Update prices for each timeframe and return (used for stocks cache update)
        List<AbstractPriceOHLC> htfPricesUpdated = new ArrayList<>();
        List<WeeklyPriceOHLC> weeklyPrices = updateAndSavePrices(importedDailyPrices, WEEKLY, previousTwoWeeklyPrices);
        List<MonthlyPriceOHLC> monthlyPrices = updateAndSavePrices(importedDailyPrices, MONTHLY, previousTwoMonthlyPrices);
        List<QuarterlyPriceOHLC> quarterlyPrices = updateAndSavePrices(importedDailyPrices, QUARTERLY, previousTwoQuarterlyPrices);
        List<YearlyPriceOHLC> yearlyPrices = updateAndSavePrices(importedDailyPrices, YEARLY, previousTwoYearlyPrices);
        htfPricesUpdated.addAll(weeklyPrices);
        htfPricesUpdated.addAll(monthlyPrices);
        htfPricesUpdated.addAll(quarterlyPrices);
        htfPricesUpdated.addAll(yearlyPrices);

        higherTimeframePricesCache.addWeeklyPrices(weeklyPrices);
        higherTimeframePricesCache.addMonthlyPrices(monthlyPrices);
        higherTimeframePricesCache.addQuarterlyPrices(quarterlyPrices);
        higherTimeframePricesCache.addYearlyPrices(yearlyPrices);

        return htfPricesUpdated;
    }

    private List<WeeklyPriceOHLC> getPreviousTwoWeeklyPrices(List<String> tickers) {
        Set<String> cacheTickers = higherTimeframePricesCache.weeklyPricesTickers();
        List<WeeklyPriceOHLC> previousWeeklyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching PreviousTwoWeeklyPrices from database for {} tickers", tickers.size());
            previousWeeklyPrices = pricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            higherTimeframePricesCache.addWeeklyPrices(previousWeeklyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousWeeklyPrices = higherTimeframePricesCache.weeklyPricesFor(tickers);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousWeeklyPrices = pricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            higherTimeframePricesCache.addWeeklyPrices(previousWeeklyPrices);
            log.info("previousWeeklyPrices partial match for {} tickers", tickers.size());
            cacheTickers.addAll(tickers);
            previousWeeklyPrices = higherTimeframePricesCache.weeklyPricesFor(cacheTickers.stream().toList());
        }

        return previousWeeklyPrices
                .stream()
                .collect(Collectors.groupingBy(WeeklyPriceOHLC::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(WeeklyPriceOHLC::getStartDate).reversed()).limit(2))
                .toList();
    }

    private List<MonthlyPriceOHLC> getPreviousTwoMonthlyPrices(List<String> tickers) {
        Set<String> cacheTickers = higherTimeframePricesCache.monthlyPricesTickers();
        List<MonthlyPriceOHLC> previousMonthlyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching PreviousTwoMonthlyPrices from database for {} tickers", tickers.size());
            previousMonthlyPrices = pricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            higherTimeframePricesCache.addMonthlyPrices(previousMonthlyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousMonthlyPrices = higherTimeframePricesCache.monthlyPricesFor(tickers);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousMonthlyPrices = pricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            higherTimeframePricesCache.addMonthlyPrices(previousMonthlyPrices);
            log.info("previousMonthlyPrices partial match for {} tickers", tickers.size());
            cacheTickers.addAll(tickers);
            previousMonthlyPrices = higherTimeframePricesCache.monthlyPricesFor(cacheTickers.stream().toList());
        }

        return previousMonthlyPrices
                .stream()
                .collect(Collectors.groupingBy(MonthlyPriceOHLC::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(MonthlyPriceOHLC::getStartDate).reversed()).limit(2))
                .toList();
    }

    private List<QuarterlyPriceOHLC> getPreviousTwoQuarterlyPrices(List<String> tickers) {
        Set<String> cacheTickers = higherTimeframePricesCache.quarterlyPricesTickers();
        List<QuarterlyPriceOHLC> previousQuarterlyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching PreviousTwoQuarterlyPrices from database for {} tickers", tickers.size());
            previousQuarterlyPrices = pricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            higherTimeframePricesCache.addQuarterlyPrices(previousQuarterlyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousQuarterlyPrices = higherTimeframePricesCache.quarterlyPricesFor(tickers);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousQuarterlyPrices = pricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            higherTimeframePricesCache.addQuarterlyPrices(previousQuarterlyPrices);
            log.info("previousQuarterlyPrices partial match for {} tickers", tickers.size());
            cacheTickers.addAll(tickers);
            previousQuarterlyPrices = higherTimeframePricesCache.quarterlyPricesFor(cacheTickers.stream().toList());
        }

        return previousQuarterlyPrices
                .stream()
                .collect(Collectors.groupingBy(QuarterlyPriceOHLC::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(QuarterlyPriceOHLC::getStartDate).reversed()).limit(2))
                .toList();
    }

    private List<YearlyPriceOHLC> getPreviousTwoYearlyPrices(List<String> tickers) {
        Set<String> cacheTickers = higherTimeframePricesCache.yearlyPricesTickers();
        List<YearlyPriceOHLC> previousYearlyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching PreviousTwoYearlyPrices from database for {} tickers", tickers.size());
            previousYearlyPrices = pricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
            higherTimeframePricesCache.addYearlyPrices(previousYearlyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousYearlyPrices = higherTimeframePricesCache.yearlyPricesFor(tickers);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousYearlyPrices = pricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
            higherTimeframePricesCache.addYearlyPrices(previousYearlyPrices);
            log.info("previousYearlyPrices partial match for {} tickers", tickers.size());
            cacheTickers.addAll(tickers);
            previousYearlyPrices = higherTimeframePricesCache.yearlyPricesFor(cacheTickers.stream().toList());
        }

        return previousYearlyPrices
                .stream()
                .collect(Collectors.groupingBy(YearlyPriceOHLC::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(YearlyPriceOHLC::getStartDate).reversed()).limit(2))
                .toList();
    }


    @SuppressWarnings("unchecked")
    private <T extends AbstractPriceOHLC> List<T> updateAndSavePrices(List<DailyPriceOHLC> importedDailyPrices,
                                                        StockTimeframe timeframe,
                                                        List<T> previousPrices) {
        Map<String, List<AbstractPriceOHLC>> previousPricesByTicker = previousPrices.stream()
                .collect(Collectors.groupingBy(AbstractPriceOHLC::getTicker, Collectors.mapping(p -> (AbstractPriceOHLC) p, Collectors.toList())));

        List<AbstractPriceOHLC> updatedPrices = updatePricesAndPerformance(importedDailyPrices, timeframe, previousPricesByTicker);
        partitionDataAndSaveNoLogging(updatedPrices, pricesRepository);

        return (List<T>) updatedPrices;
    }

    private List<AbstractPriceOHLC> updatePricesAndPerformance(List<DailyPriceOHLC> dailyPrices, StockTimeframe timeframe, Map<String, List<AbstractPriceOHLC>> previousTwoWMYPricesByTicker) {
        List<AbstractPriceOHLC> wmyPrices = new ArrayList<>();
        for (DailyPriceOHLC dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            if (previousTwoWMYPricesByTicker.containsKey(ticker)) {
                wmyPrices.add(wmyPriceUpdatedFrom(previousTwoWMYPricesByTicker.get(ticker), dailyPrice, timeframe));
            } else { // IPO first day, create new weekly, monthly, yearly
                wmyPrices.add(createNewWMYPrice(dailyPrice, timeframe, dailyPrice.getOpen()));
            }
        }
        return wmyPrices;
    }

    private AbstractPriceOHLC wmyPriceUpdatedFrom(List<AbstractPriceOHLC> previousTwoWMY, DailyPriceOHLC dailyPrice, StockTimeframe timeframe) {
        AbstractPriceOHLC result;
        AbstractPriceOHLC latestPriceWMY = previousTwoWMY.getFirst();
        LocalDate latestEndDateWMY = latestPriceWMY.getEndDate();
        LocalDate dailyPriceDate = dailyPrice.getDate();
        if (latestEndDateWMY.equals(dailyPriceDate)) { // already imported (intraday update prices, performance)
            // check for IPO week, month, year by size < 2
            double previousClose = previousTwoWMY.size() < 2 ? latestPriceWMY.getOpen() : previousTwoWMY.getLast().getClose();
            result = latestPriceWMY.convertFrom(dailyPrice, previousClose);
        } else {
            result = switch (timeframe) {
                case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
                case WEEKLY, MONTHLY, QUARTERLY, YEARLY ->
                        updateOrCreateWMYPrice(previousTwoWMY, dailyPrice, latestEndDateWMY, timeframe);
            };
        }
        return result;
    }

    private AbstractPriceOHLC updateOrCreateWMYPrice(List<AbstractPriceOHLC> previousTwoWMY, DailyPriceOHLC dailyPrice, LocalDate latestEndDateWMY, StockTimeframe timeframe) {
        AbstractPriceOHLC result;
        LocalDate dailyPriceDate = dailyPrice.getDate();
        AbstractPriceOHLC latestPriceWMY = previousTwoWMY.getFirst();
        if (isWithinSameTimeframe(dailyPriceDate, latestEndDateWMY, timeframe)) {
            double previousClose = previousTwoWMY.size() < 2 ? latestPriceWMY.getOpen() : previousTwoWMY.getLast().getClose();
            result = latestPriceWMY.convertFrom(dailyPrice, previousClose);
            setEndDate(result, dailyPriceDate, timeframe);
        } else { // new week, month, year
            double previousClose = latestPriceWMY.getClose();
            result = createNewWMYPrice(dailyPrice, timeframe, previousClose);
        }
        return result;
    }

    private boolean isWithinSameTimeframe(LocalDate date, LocalDate latestEndDateWMY, StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> sameWeek(date, latestEndDateWMY);
            case MONTHLY -> sameMonth(date, latestEndDateWMY);
            case QUARTERLY -> sameQuarter(date, latestEndDateWMY);
            case YEARLY -> sameYear(date, latestEndDateWMY);
        };
    }

    private AbstractPriceOHLC createNewWMYPrice(DailyPriceOHLC dailyPrice, StockTimeframe timeframe, double previousClose) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> WeeklyPriceOHLC.newFrom(dailyPrice, previousClose);
            case MONTHLY -> MonthlyPriceOHLC.newFrom(dailyPrice, previousClose);
            case QUARTERLY -> QuarterlyPriceOHLC.newFrom(dailyPrice, previousClose);
            case YEARLY -> YearlyPriceOHLC.newFrom(dailyPrice, previousClose);
        };
    }

    private void setEndDate(AbstractPriceOHLC result, LocalDate endDate, StockTimeframe timeframe) {
        switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> ((WeeklyPriceOHLC) result).setEndDate(endDate);
            case MONTHLY -> ((MonthlyPriceOHLC) result).setEndDate(endDate);
            case QUARTERLY -> ((QuarterlyPriceOHLC) result).setEndDate(endDate);
            case YEARLY -> ((YearlyPriceOHLC) result).setEndDate(endDate);
        }
    }

    @Transactional
    public void updateAllHigherTimeframesPricesForTickers(LocalDate date, String tickers) {
        updateHigherTimeframesPricesFor(date, higherTimeframes(), tickers);
    }

    @Transactional
    public void updateHigherTimeframesPricesFor(LocalDate date, List<StockTimeframe> timeframes, String tickers) {
        for (StockTimeframe timeframe : timeframes) {
            updateHigherTimeframeHistPrices(timeframe.toDateTruncPeriod(), timeframe.sequenceName(), timeframe.toSQLInterval(), timeframe.dbTableOHLC(), timeframe.htfDateFrom(date), tickers);
        }
    }

    private void updateHigherTimeframeHistPrices(String dateTruncPeriod, String sequenceName, String sqlInterval, String tableName, LocalDate date, String tickers) {
        String dateFormatted = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        int savedOrUpdatedCount = entityManager.createNativeQuery(
                queryFrom(tickers, dateTruncPeriod, sequenceName, sqlInterval, tableName, dateFormatted)
        ).executeUpdate();
        if (savedOrUpdatedCount != 0) {
            log.info("saved/updated {} {} rows for date {} and tickers {}", savedOrUpdatedCount, dateTruncPeriod, dateFormatted, tickers);
        }
    }

    private String queryFrom(String tickers, String dateTruncPeriod, String sequenceName, String sqlInterval, String tableName, String dateFormatted) {
        String query = STR."""
            WITH interval_data AS (
            SELECT
                ticker,
                MIN(date) AS start_date,
                MAX(date) AS end_date,
                MAX(high) AS high,
                MIN(low) AS low
            FROM daily_prices
            WHERE date BETWEEN '\{dateFormatted}'::date - INTERVAL '\{sqlInterval}' AND '\{dateFormatted}'
            """;

        if (!tickers.isEmpty()) {
            query = query.concat(
                    STR."""
                    AND ticker in (\{tickers})
                    """);
        }

        query = query.concat(STR."""
            GROUP BY ticker, DATE_TRUNC('\{dateTruncPeriod}', date)
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
                    WHERE start_date >= DATE_TRUNC('\{dateTruncPeriod}', '\{dateFormatted}'::date)
            )
            INSERT INTO \{tableName} (id, ticker, start_date, end_date, high, low, open, close, performance)
            SELECT nextval('\{sequenceName}') AS id, ticker, DATE_TRUNC('\{dateTruncPeriod}', start_date), end_date, high, low, open, close, performance
            FROM final_result
            ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                    open = EXCLUDED.open,
                    high = EXCLUDED.high,
                    low = EXCLUDED.low,
                    close = EXCLUDED.close,
                    performance = EXCLUDED.performance,
                    end_date = EXCLUDED.end_date
            """);

        return query;
    }

    @Transactional
    public void savePrices(List<? extends AbstractPriceOHLC> prices) {
        pricesRepository.saveAll(prices);
    }
}
