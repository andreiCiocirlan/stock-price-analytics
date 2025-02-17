package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.DailyPricesCache;
import stock.price.analytics.cache.HigherTimeframePricesCache;
import stock.price.analytics.controller.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.*;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;
import static stock.price.analytics.util.Constants.DAILY_FVG_MIN_DATE;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSaveNoLogging;
import static stock.price.analytics.util.StockDateUtils.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class PricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PricesRepository pricesRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final QuarterlyPricesRepository quarterlyPricesRepository;
    private final YearlyPricesRepository yearlyPricesRepository;
    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final DailyPricesCache dailyPricesCache;

    public List<AbstractPrice> currentCachePricesFor(StockTimeframe timeframe) {
        List<AbstractPrice> htfPricesUpdated = new ArrayList<>(switch (timeframe) {
            case DAILY -> new ArrayList<>(dailyPricesCache.dailyPrices(REGULAR));
            case WEEKLY -> new ArrayList<>(higherTimeframePricesCache.getWeeklyPricesByTickerAndDate().values());
            case MONTHLY -> new ArrayList<>(higherTimeframePricesCache.getMonthlyPricesByTickerAndDate().values());
            case QUARTERLY -> new ArrayList<>(higherTimeframePricesCache.getQuarterlyPricesByTickerAndDate().values());
            case YEARLY -> new ArrayList<>(higherTimeframePricesCache.getYearlyPricesByTickerAndDate().values());
        });

        return htfPricesUpdated.stream()
                .collect(Collectors.groupingBy(AbstractPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(AbstractPrice::getStartDate).reversed()).limit(1))
                .toList();
    }

    public Set<String> cacheTickersFor(StockTimeframe timeframe) {
        return (switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> higherTimeframePricesCache.getWeeklyPricesByTickerAndDate();
            case MONTHLY -> higherTimeframePricesCache.getMonthlyPricesByTickerAndDate();
            case QUARTERLY -> higherTimeframePricesCache.getQuarterlyPricesByTickerAndDate();
            case YEARLY -> higherTimeframePricesCache.getYearlyPricesByTickerAndDate();
        }).keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toSet());
    }

    public List<? extends AbstractPrice> previousThreePricesFor(List<String> tickers, StockTimeframe timeframe) {
        return (switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            case MONTHLY -> monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            case QUARTERLY -> quarterlyPricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            case YEARLY -> yearlyPricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
        });
    }

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

    public List<? extends AbstractPrice> findAllPricesFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> dailyPricesRepository.findByDateBetween(DAILY_FVG_MIN_DATE, LocalDate.now());
            case WEEKLY -> weeklyPricesRepository.findAll();
            case MONTHLY -> monthlyPricesRepository.findAll();
            case QUARTERLY -> quarterlyPricesRepository.findAll();
            case YEARLY -> yearlyPricesRepository.findAll();
        };
    }

    public List<AbstractPrice> updatePricesForHigherTimeframes(List<DailyPrice> importedDailyPrices) {
        return logTimeAndReturn(() -> updateHTF(importedDailyPrices), "updated prices for higher timeframes");
    }

    private List<AbstractPrice> updateHTF(List<DailyPrice> importedDailyPrices) {
        List<String> tickers = new ArrayList<>(importedDailyPrices.stream().map(DailyPrice::getTicker).toList());

        // Fetch previous prices for each timeframe
        List<WeeklyPrice> previousTwoWeeklyPrices = getPreviousTwoWeeklyPrices(tickers, WEEKLY);
        List<MonthlyPrice> previousTwoMonthlyPrices = getPreviousTwoMonthlyPrices(tickers, MONTHLY);
        List<QuarterlyPrice> previousTwoQuarterlyPrices = getPreviousTwoQuarterlyPrices(tickers, QUARTERLY);
        List<YearlyPrice> previousTwoYearlyPrices = getPreviousTwoYearlyPrices(tickers, YEARLY);

        // Update prices for each timeframe and return (used for stocks cache update)
        List<AbstractPrice> htfPricesUpdated = new ArrayList<>();
        List<WeeklyPrice> weeklyPrices = updateAndSavePrices(importedDailyPrices, WEEKLY, previousTwoWeeklyPrices);
        List<MonthlyPrice> monthlyPrices = updateAndSavePrices(importedDailyPrices, MONTHLY, previousTwoMonthlyPrices);
        List<QuarterlyPrice> quarterlyPrices = updateAndSavePrices(importedDailyPrices, QUARTERLY, previousTwoQuarterlyPrices);
        List<YearlyPrice> yearlyPrices = updateAndSavePrices(importedDailyPrices, YEARLY, previousTwoYearlyPrices);
        htfPricesUpdated.addAll(weeklyPrices);
        htfPricesUpdated.addAll(monthlyPrices);
        htfPricesUpdated.addAll(quarterlyPrices);
        htfPricesUpdated.addAll(yearlyPrices);

        higherTimeframePricesCache.addPrices(weeklyPrices);
        higherTimeframePricesCache.addPrices(monthlyPrices);
        higherTimeframePricesCache.addPrices(quarterlyPrices);
        higherTimeframePricesCache.addPrices(yearlyPrices);

        return htfPricesUpdated;
    }

    @SuppressWarnings("unchecked")
    private List<WeeklyPrice> getPreviousTwoWeeklyPrices(List<String> tickers, StockTimeframe timeframe) {
        Set<String> cacheTickers = cacheTickersFor(timeframe);
        List<WeeklyPrice> previousWeeklyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching Previous Two {} Prices from database for {} tickers", timeframe.name(), tickers.size());
            previousWeeklyPrices = weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousWeeklyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousWeeklyPrices = (List<WeeklyPrice>) higherTimeframePricesCache.pricesFor(tickers, timeframe);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousWeeklyPrices = weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousWeeklyPrices);
            log.info("previous {} Prices partial match for {} tickers", timeframe.name(), tickers.size());
            cacheTickers.addAll(tickers);
            previousWeeklyPrices = (List<WeeklyPrice>) higherTimeframePricesCache.pricesFor(cacheTickers.stream().toList(), timeframe);
        }

        return previousWeeklyPrices
                .stream()
                .collect(Collectors.groupingBy(WeeklyPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(WeeklyPrice::getStartDate).reversed()).limit(2))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<MonthlyPrice> getPreviousTwoMonthlyPrices(List<String> tickers, StockTimeframe timeframe) {
        Set<String> cacheTickers = cacheTickersFor(timeframe);
        List<MonthlyPrice> previousMonthlyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching Previous Two {} Prices from database for {} tickers", timeframe.name(), tickers.size());
            previousMonthlyPrices = monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousMonthlyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousMonthlyPrices = (List<MonthlyPrice>) higherTimeframePricesCache.pricesFor(tickers, timeframe);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousMonthlyPrices = monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousMonthlyPrices);
            log.info("previous {} Prices partial match for {} tickers", timeframe.name(), tickers.size());
            cacheTickers.addAll(tickers);
            previousMonthlyPrices = (List<MonthlyPrice>) higherTimeframePricesCache.pricesFor(cacheTickers.stream().toList(), timeframe);
        }

        return previousMonthlyPrices
                .stream()
                .collect(Collectors.groupingBy(MonthlyPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(MonthlyPrice::getStartDate).reversed()).limit(2))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<QuarterlyPrice> getPreviousTwoQuarterlyPrices(List<String> tickers, StockTimeframe timeframe) {
        Set<String> cacheTickers = cacheTickersFor(timeframe);
        List<QuarterlyPrice> previousQuarterlyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching Previous Two {} Prices from database for {} tickers", timeframe.name(), tickers.size());
            previousQuarterlyPrices = quarterlyPricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousQuarterlyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousQuarterlyPrices = (List<QuarterlyPrice>) higherTimeframePricesCache.pricesFor(tickers, timeframe);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousQuarterlyPrices = quarterlyPricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousQuarterlyPrices);
            log.info("previous {} Prices partial match for {} tickers", timeframe.name(), tickers.size());
            cacheTickers.addAll(tickers);
            previousQuarterlyPrices = (List<QuarterlyPrice>) higherTimeframePricesCache.pricesFor(cacheTickers.stream().toList(), timeframe);
        }

        return previousQuarterlyPrices
                .stream()
                .collect(Collectors.groupingBy(QuarterlyPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(QuarterlyPrice::getStartDate).reversed()).limit(2))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<YearlyPrice> getPreviousTwoYearlyPrices(List<String> tickers, StockTimeframe timeframe) {
        Set<String> cacheTickers = cacheTickersFor(timeframe);
        List<YearlyPrice> previousYearlyPrices;
        if (cacheTickers.isEmpty()) {
            log.info("Fetching Previous Two {} Prices from database for {} tickers", timeframe.name(), tickers.size());
            previousYearlyPrices = yearlyPricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousYearlyPrices);
        } else if (cacheTickers.containsAll(tickers)) {
            previousYearlyPrices = (List<YearlyPrice>) higherTimeframePricesCache.pricesFor(tickers, timeframe);
        } else { // partial match
            tickers.removeAll(cacheTickers);
            previousYearlyPrices = yearlyPricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
            higherTimeframePricesCache.addPrices(previousYearlyPrices);
            log.info("previous {} Prices partial match for {} tickers", timeframe.name(), tickers.size());
            cacheTickers.addAll(tickers);
            previousYearlyPrices = (List<YearlyPrice>) higherTimeframePricesCache.pricesFor(cacheTickers.stream().toList(), timeframe);
        }

        return previousYearlyPrices
                .stream()
                .collect(Collectors.groupingBy(YearlyPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(YearlyPrice::getStartDate).reversed()).limit(2))
                .toList();
    }


    @SuppressWarnings("unchecked")
    private <T extends AbstractPrice> List<T> updateAndSavePrices(List<DailyPrice> importedDailyPrices,
                                                                  StockTimeframe timeframe,
                                                                  List<T> previousPrices) {
        Map<String, List<AbstractPrice>> previousPricesByTicker = previousPrices.stream()
                .collect(Collectors.groupingBy(AbstractPrice::getTicker, Collectors.mapping(p -> (AbstractPrice) p, Collectors.toList())));

        List<AbstractPrice> updatedPrices = updatePricesAndPerformance(importedDailyPrices, timeframe, previousPricesByTicker);
        partitionDataAndSaveNoLogging(updatedPrices, pricesRepository);

        return (List<T>) updatedPrices;
    }

    private List<AbstractPrice> updatePricesAndPerformance(List<DailyPrice> dailyPrices, StockTimeframe timeframe, Map<String, List<AbstractPrice>> previousTwoWMYPricesByTicker) {
        List<AbstractPrice> wmyPrices = new ArrayList<>();
        for (DailyPrice dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            if (previousTwoWMYPricesByTicker.containsKey(ticker)) {
                wmyPrices.add(wmyPriceUpdatedFrom(previousTwoWMYPricesByTicker.get(ticker), dailyPrice, timeframe));
            } else { // IPO first day, create new weekly, monthly, yearly
                wmyPrices.add(createNewWMYPrice(dailyPrice, timeframe, dailyPrice.getOpen()));
            }
        }
        return wmyPrices;
    }

    private AbstractPrice wmyPriceUpdatedFrom(List<AbstractPrice> previousTwoWMY, DailyPrice dailyPrice, StockTimeframe timeframe) {
        AbstractPrice result;
        AbstractPrice latestPriceWMY = previousTwoWMY.getFirst();
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

    private AbstractPrice updateOrCreateWMYPrice(List<AbstractPrice> previousTwoWMY, DailyPrice dailyPrice, LocalDate latestEndDateWMY, StockTimeframe timeframe) {
        AbstractPrice result;
        LocalDate dailyPriceDate = dailyPrice.getDate();
        AbstractPrice latestPriceWMY = previousTwoWMY.getFirst();
        if (isWithinSameTimeframe(dailyPriceDate, latestEndDateWMY, timeframe)) {
            double previousClose = previousTwoWMY.size() < 2 ? latestPriceWMY.getOpen() : previousTwoWMY.getLast().getClose();
            result = latestPriceWMY.convertFrom(dailyPrice, previousClose);
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

    private AbstractPrice createNewWMYPrice(DailyPrice dailyPrice, StockTimeframe timeframe, double previousClose) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> WeeklyPrice.newFrom(dailyPrice, previousClose);
            case MONTHLY -> MonthlyPrice.newFrom(dailyPrice, previousClose);
            case QUARTERLY -> QuarterlyPrice.newFrom(dailyPrice, previousClose);
            case YEARLY -> YearlyPrice.newFrom(dailyPrice, previousClose);
        };
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
    public void savePrices(List<? extends AbstractPrice> prices) {
        partitionDataAndSave(prices, pricesRepository);
    }
}
