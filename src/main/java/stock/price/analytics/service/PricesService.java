package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.cache.model.*;
import stock.price.analytics.controller.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.ohlc.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.DAILY;
import static stock.price.analytics.model.prices.enums.StockTimeframe.higherTimeframes;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;
import static stock.price.analytics.util.PricesUtil.pricesWithPerformance;
import static stock.price.analytics.util.TradingDateUtil.isWithinSameTimeframe;


@Slf4j
@Service
@RequiredArgsConstructor
public class PricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PricesRepository pricesRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final QuarterlyPricesRepository quarterlyPricesRepository;
    private final YearlyPricesRepository yearlyPricesRepository;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;


    @SuppressWarnings("unchecked")
    public List<stock.price.analytics.model.prices.ohlc.AbstractPrice> previousThreePricesFor(List<String> tickers, StockTimeframe timeframe) {
        return (List<stock.price.analytics.model.prices.ohlc.AbstractPrice>) (switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            case MONTHLY -> monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            case QUARTERLY -> quarterlyPricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            case YEARLY -> yearlyPricesRepository.findPreviousThreeYearlyPricesForTickers(tickers);
        });
    }

    public boolean isFirstImportFor(StockTimeframe timeframe) {
        String timeframePeriod = timeframe.toDateTruncPeriod();
        String query = STR."""
                SELECT
                    CASE
                        WHEN COUNT(*) = 0 THEN TRUE
                        ELSE FALSE
                    END AS result
                FROM
                    daily_prices
                WHERE
                    ticker = 'AAPL'
                    AND date_trunc('\{timeframePeriod}', date) = date_trunc('\{timeframePeriod}', current_date);
                """;

        Query nativeQuery = entityManager.createNativeQuery(query, Boolean.class);

        return (Boolean) nativeQuery.getResultList().getFirst();
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

    public List<AbstractPrice> updatePricesForHigherTimeframes(List<DailyPrice> importedDailyPrices) {
        return logTimeAndReturn(() -> updateHTF(importedDailyPrices), "updated prices for higher timeframes");
    }

    @Transactional
    private List<AbstractPrice> updateHTF(List<DailyPrice> importedDailyPrices) {
        List<String> tickers = new ArrayList<>(importedDailyPrices.stream().map(DailyPrice::getTicker).toList());

        // Update prices for each timeframe and return (used for stocks cache update)
        List<AbstractPrice> htfPricesUpdated = new ArrayList<>();
        for (StockTimeframe timeframe : higherTimeframes()) {
            List<PriceWithPrevClose> htfPricesWithPrevCloseUpdated = updateAndSavePrices(importedDailyPrices, timeframe,
                    cacheService.htfPricesWithPrevCloseFor(tickers, timeframe));
            htfPricesUpdated.addAll(htfPricesWithPrevCloseUpdated.stream().map(PriceWithPrevClose::getPrice).toList());
            cacheService.addHtfPricesWithPrevClose(htfPricesWithPrevCloseUpdated);
        }
        asyncPersistenceService.partitionDataAndSaveWithLogTime(htfPricesUpdated, pricesRepository, "saved HTF prices");

        return htfPricesUpdated;
    }


    private List<PriceWithPrevClose> updateAndSavePrices(List<DailyPrice> importedDailyPrices,
                                                         StockTimeframe timeframe,
                                                         List<PriceWithPrevClose> pricesWithPrevClose) {
        List<PriceWithPrevClose> result = new ArrayList<>();
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevClose.stream()
                .collect(Collectors.toMap(priceWithPrevClose -> priceWithPrevClose.getPrice().getTicker(), p -> p));
        for (DailyPrice importedDailyPrice : importedDailyPrices) {
            String ticker = importedDailyPrice.getTicker();
            PriceWithPrevClose priceWithPrevClose = pricesWithPrevCloseByTicker.get(ticker);
            AbstractPrice price = priceWithPrevClose.getPrice();
            LocalDate latestEndDateWMQY = price.getEndDate(); // latest cached w,m,q,y end_date per ticker
            if (isWithinSameTimeframe(importedDailyPrice.getDate(), latestEndDateWMQY, timeframe)) {
                price.convertFrom(importedDailyPrice, priceWithPrevClose.previousClose());
                result.add(priceWithPrevClose);
            } else { // new week, month, quarter, year
                result.add(newPriceWithPrevCloseFrom(importedDailyPrice, timeframe, price.getClose()));
            }
        }

        return result;
    }

    private PriceWithPrevClose newPriceWithPrevCloseFrom(DailyPrice importedDailyPrice, StockTimeframe timeframe, double previousClose) {
        AbstractPrice price = createNewHTFPrice(importedDailyPrice, timeframe, previousClose);

        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected timeframe DAILY");
            case WEEKLY -> new WeeklyPriceWithPrevClose((WeeklyPrice) price, previousClose);
            case MONTHLY -> new MonthlyPriceWithPrevClose((MonthlyPrice) price, previousClose);
            case QUARTERLY -> new QuarterlyPriceWithPrevClose((QuarterlyPrice) price, previousClose);
            case YEARLY -> new YearlyPriceWithPrevClose((YearlyPrice) price, previousClose);
        };
    }

    private AbstractPrice createNewHTFPrice(DailyPrice dailyPrice, StockTimeframe timeframe, double previousClose) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> WeeklyPrice.newFrom(dailyPrice, previousClose);
            case MONTHLY -> MonthlyPrice.newFrom(dailyPrice, previousClose);
            case QUARTERLY -> QuarterlyPrice.newFrom(dailyPrice, previousClose);
            case YEARLY -> YearlyPrice.newFrom(dailyPrice, previousClose);
        };
    }

    @Transactional
    public void updateHtfPricesPerformanceFor(LocalDate date, String ticker) {
        List<AbstractPrice> updatedPrices = new ArrayList<>();
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            List<? extends AbstractPrice> currentAndPreviousPriceForTimeframeSorted = findCurrentAndPrevHTFPricesFor(date, timeframe, ticker).stream()
                    .sorted(Comparator.comparing(AbstractPrice::getStartDate)).toList();
            // updates only the price performance within timeframe of date
            List<? extends AbstractPrice> updated = pricesWithPerformance(currentAndPreviousPriceForTimeframeSorted);
            updatedPrices.addAll(updated);
        }
        asyncPersistenceService.partitionDataAndSave(updatedPrices, pricesRepository);
    }

    public List<? extends AbstractPrice> findCurrentAndPrevHTFPricesFor(LocalDate date, StockTimeframe timeframe, String ticker) {
        LocalDate from;
        LocalDate to;

        return switch (timeframe) {
            case WEEKLY:
                from = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                to = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
                yield weeklyPricesRepository.findByTickerAndStartDateBetween(ticker, from, to);
            case MONTHLY:
                from = date.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(1);
                to = date.with(TemporalAdjusters.firstDayOfMonth());
                yield monthlyPricesRepository.findByTickerAndStartDateBetween(ticker, from, to);
            case QUARTERLY:
                int firstMonthOfQuarter = date.getMonth().firstMonthOfQuarter().getValue();
                from = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).minusMonths(3);
                to = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1);
                yield quarterlyPricesRepository.findByTickerAndStartDateBetween(ticker, from, to);
            case YEARLY:
                from = date.with(TemporalAdjusters.firstDayOfYear()).minusYears(1);
                to = date.with(TemporalAdjusters.firstDayOfYear());
                yield yearlyPricesRepository.findByTickerAndStartDateBetween(ticker, from, to);
            case DAILY:
                throw new IllegalArgumentException("Unsupported timeframe: DAILY");
        };
    }

    @Transactional
    public void savePrices(List<? extends AbstractPrice> prices) {
        asyncPersistenceService.partitionDataAndSave(prices, pricesRepository);
    }
}
