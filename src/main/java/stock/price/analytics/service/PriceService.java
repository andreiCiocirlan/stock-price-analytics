package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.ohlc.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.temporal.TemporalAdjusters.*;
import static stock.price.analytics.model.prices.enums.StockTimeframe.*;
import static stock.price.analytics.util.PricesUtil.htfPricesForTimeframe;
import static stock.price.analytics.util.TradingDateUtil.isWithinSameTimeframe;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;


@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final PriceRepository priceRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final WeeklyPriceRepository weeklyPriceRepository;
    private final MonthlyPriceRepository monthlyPriceRepository;
    private final QuarterlyPriceRepository quarterlyPriceRepository;
    private final YearlyPriceRepository yearlyPriceRepository;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;
    private final SyncPersistenceService syncPersistenceService;

    public void adjustPricesFor(String ticker, LocalDate stockSplitDate, double priceMultiplier) {
        List<DailyPrice> dailyPricesToUpdate = dailyPriceRepository.findByTickerAndDateLessThanEqual(ticker, tradingDateNow());
        LocalDate splitDateWeeklyCutoff = stockSplitDate.with(previousOrSame(DayOfWeek.MONDAY));
        LocalDate splitDateMonthlyCutoff = stockSplitDate.with(firstDayOfMonth());
        LocalDate splitDateQuarterlyCutoff = LocalDate.of(stockSplitDate.getYear(), stockSplitDate.getMonth().firstMonthOfQuarter().getValue(), 1);
        LocalDate splitDateYearlyCutoff = stockSplitDate.with(firstDayOfYear());
        List<WeeklyPrice> weeklyPricesToUpdate = weeklyPriceRepository.findByTickerAndStartDateLessThanEqual(ticker, splitDateWeeklyCutoff);
        List<MonthlyPrice> monthlyPricesToUpdate = monthlyPriceRepository.findByTickerAndStartDateLessThanEqual(ticker, splitDateMonthlyCutoff);
        List<QuarterlyPrice> quarterlyPricesToUpdate = quarterlyPriceRepository.findByTickerAndStartDateLessThanEqual(ticker, splitDateQuarterlyCutoff);
        List<YearlyPrice> yearlyPricesToUpdate = yearlyPriceRepository.findByTickerAndStartDateLessThanEqual(ticker, splitDateYearlyCutoff);

        // update htf prices before stockSplitDate
        List<WeeklyPrice> updatedWeeklyPrices = weeklyPricesToUpdate.stream()
                .map(p -> p.getStartDate().isBefore(splitDateWeeklyCutoff) ? (WeeklyPrice) updatePrice(p, priceMultiplier) : p)
                .toList();
        List<MonthlyPrice> updatedMonthlyPrices = monthlyPricesToUpdate.stream()
                .map(p -> p.getStartDate().isBefore(splitDateMonthlyCutoff) ? (MonthlyPrice) updatePrice(p, priceMultiplier) : p)
                .toList();
        List<QuarterlyPrice> updatedQuarterlyPrices = quarterlyPricesToUpdate.stream()
                .map(p -> p.getStartDate().isBefore(splitDateQuarterlyCutoff) ? (QuarterlyPrice) updatePrice(p, priceMultiplier) : p)
                .toList();
        List<YearlyPrice> updatedYearlyPrices = yearlyPricesToUpdate.stream()
                .map(p -> p.getStartDate().isBefore(splitDateYearlyCutoff) ? (YearlyPrice) updatePrice(p, priceMultiplier) : p)
                .toList();

        // Update daily prices before stockSplitDate and keep others unchanged
        List<DailyPrice> updatedDailyPrices = dailyPricesToUpdate.stream()
                .map(dp -> dp.getDate().isBefore(stockSplitDate) ? (DailyPrice) updatePrice(dp, priceMultiplier) : dp)
                .toList();

        syncPersistenceService.partitionDataAndSave(updatedDailyPrices.stream().filter(price -> price.getDate().isBefore(stockSplitDate)).toList(), dailyPriceRepository);
        syncPersistenceService.partitionDataAndSave(weeklyPricesToUpdate, weeklyPriceRepository);
        syncPersistenceService.partitionDataAndSave(monthlyPricesToUpdate, monthlyPriceRepository);
        syncPersistenceService.partitionDataAndSave(quarterlyPricesToUpdate, quarterlyPriceRepository);
        syncPersistenceService.partitionDataAndSave(yearlyPricesToUpdate, yearlyPriceRepository);

        Map<StockTimeframe, AbstractPrice> htfPriceByTimeframe = new HashMap<>();
        htfPriceByTimeframe.put(WEEKLY, updatedWeeklyPrices.stream().filter(dp -> dp.getStartDate().isEqual(splitDateWeeklyCutoff)).findFirst().orElseThrow());
        htfPriceByTimeframe.put(MONTHLY, updatedMonthlyPrices.stream().filter(dp -> dp.getStartDate().isEqual(splitDateMonthlyCutoff)).findFirst().orElseThrow());
        htfPriceByTimeframe.put(QUARTERLY, updatedQuarterlyPrices.stream().filter(dp -> dp.getStartDate().isEqual(splitDateQuarterlyCutoff)).findFirst().orElseThrow());
        htfPriceByTimeframe.put(YEARLY, updatedYearlyPrices.stream().filter(dp -> dp.getStartDate().isEqual(splitDateYearlyCutoff)).findFirst().orElseThrow());

        List<AbstractPrice> htfPriceForStockSplitDate = computeHTFPriceForStockSplitDate(stockSplitDate, updatedDailyPrices, htfPriceByTimeframe);
        syncPersistenceService.partitionDataAndSave(htfPriceForStockSplitDate, priceRepository);
    }

    private AbstractPrice updatePrice(AbstractPrice price, double priceMultiplier) {
        price.setOpen(Math.round((priceMultiplier * price.getOpen()) * 100.0) / 100.0);
        price.setHigh(Math.round((priceMultiplier * price.getHigh()) * 100.0) / 100.0);
        price.setLow(Math.round((priceMultiplier * price.getLow()) * 100.0) / 100.0);
        price.setClose(Math.round((priceMultiplier * price.getClose()) * 100.0) / 100.0);
        return price;
    }

    public List<? extends AbstractPrice> previousThreePricesFor(List<String> tickers, StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> findPreviousThreeDailyPricesForTickers(tickers);
            case WEEKLY -> weeklyPriceRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            case MONTHLY -> monthlyPriceRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            case QUARTERLY -> quarterlyPriceRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            case YEARLY -> yearlyPriceRepository.findPreviousThreeYearlyPricesForTickers(tickers);
        };
    }

    private List<? extends AbstractPrice> findPreviousThreeDailyPricesForTickers(List<String> tickers) {
        List<DailyPrice> previousSevenDailyPricesForTickers = dailyPriceRepository.findPreviousSevenDailyPricesForTickers(tickers);

        return previousSevenDailyPricesForTickers
                .stream()
                .collect(Collectors.groupingBy(DailyPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(AbstractPrice::getStartDate).reversed()).limit(3))
                .toList();
    }

    public boolean isFirstImportDoneFor(StockTimeframe timeframe) {
        return checkImportStatusFor(timeframe, false);
    }

    public boolean isFirstImportFor(StockTimeframe timeframe) {
        return checkImportStatusFor(timeframe, true);
    }

    // checkFirstImport false -> first import not done for timeframe
    // checkFirstImport true -> first import done for timeframe
    private boolean checkImportStatusFor(StockTimeframe timeframe, boolean checkFirstImport) {
        String timeframePeriod = timeframe.toDateTruncPeriod();
        String query = STR."""
                SELECT
                    COUNT(*) = \{checkFirstImport ? "0" : "1"}
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

    @Transactional
    public void updateAllTimeframePrices(List<DailyPrice> importedDailyPrices) {
        List<String> tickers = new ArrayList<>(importedDailyPrices.stream().map(DailyPrice::getTicker).toList());

        // Update prices for each timeframe
        List<AbstractPrice> pricesUpdated = new ArrayList<>();
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            List<PriceWithPrevClose> pricesWithPrevCloseUpdated = updateAndSavePrices(importedDailyPrices, timeframe,
                    cacheService.pricesWithPrevCloseFor(tickers, timeframe));
            pricesUpdated.addAll(pricesWithPrevCloseUpdated.stream().map(PriceWithPrevClose::abstractPrice).toList());
            cacheService.addPricesWithPrevClose(pricesWithPrevCloseUpdated);
        }
        asyncPersistenceService.partitionDataAndSaveWithLogTime(pricesUpdated, priceRepository, "saved " + pricesUpdated.size() + " prices");
    }


    private List<PriceWithPrevClose> updateAndSavePrices(List<DailyPrice> importedDailyPrices,
                                                         StockTimeframe timeframe,
                                                         List<PriceWithPrevClose> pricesWithPrevClose) {
        List<PriceWithPrevClose> result = new ArrayList<>();
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevClose.stream()
                .collect(Collectors.toMap(priceWithPrevClose -> priceWithPrevClose.abstractPrice().getTicker(), Function.identity()));
        for (DailyPrice importedDailyPrice : importedDailyPrices) {
            String ticker = importedDailyPrice.getTicker();
            PriceWithPrevClose priceWithPrevClose = pricesWithPrevCloseByTicker.get(ticker);
            AbstractPrice price = priceWithPrevClose.abstractPrice();
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
        AbstractPrice price = switch (timeframe) {
            case DAILY -> DailyPrice.newFrom(importedDailyPrice, previousClose);
            case WEEKLY -> WeeklyPrice.newFrom(importedDailyPrice, previousClose);
            case MONTHLY -> MonthlyPrice.newFrom(importedDailyPrice, previousClose);
            case QUARTERLY -> QuarterlyPrice.newFrom(importedDailyPrice, previousClose);
            case YEARLY -> YearlyPrice.newFrom(importedDailyPrice, previousClose);
        };
        return new PriceWithPrevClose(price, previousClose);
    }

    public List<AbstractPrice> computeHTFPriceForStockSplitDate(LocalDate date, List<DailyPrice> dailyPrices, Map<StockTimeframe, AbstractPrice> stockSplitDateHTFPriceByTimeframe) {
        List<AbstractPrice> res = new ArrayList<>();

        for (StockTimeframe timeframe : higherTimeframes()) {
            final LocalDate from;
            final LocalDate to;
            switch (timeframe) {
                case WEEKLY:
                    from = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                    to = date.with(nextOrSame(DayOfWeek.FRIDAY));
                    break;
                case MONTHLY:
                    from = date.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(1);
                    to = date.with(lastDayOfMonth());
                    break;
                case QUARTERLY:
                    int firstMonthOfQuarter = date.getMonth().firstMonthOfQuarter().getValue();
                    from = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).minusMonths(3);
                    to = LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).plusMonths(2).with(lastDayOfMonth());
                    break;
                case YEARLY:
                    from = date.with(TemporalAdjusters.firstDayOfYear()).minusYears(1);
                    to = date.with(lastDayOfYear());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
            }

            List<DailyPrice> dailyPricesForTimeframe = dailyPrices.stream()
                    .filter(dp -> (dp.getDate().isEqual(from) || dp.getDate().isAfter(from)) && (dp.getDate().isEqual(to) || dp.getDate().isBefore(to)))
                    .sorted(Comparator.comparing(AbstractPrice::getStartDate))
                    .toList();

            AbstractPrice htfPriceUpdated = htfPricesForTimeframe(dailyPricesForTimeframe, timeframe).stream()
                    .max(Comparator.comparing(AbstractPrice::getStartDate)) // only update htf price within stock split date
                    .orElseThrow();

            AbstractPrice htfPrice = stockSplitDateHTFPriceByTimeframe.get(htfPriceUpdated.getTimeframe());
            htfPrice.updateFrom(htfPriceUpdated);
            res.add(htfPrice);
        }

        return res;
    }

    @Transactional
    public void savePrices(List<? extends AbstractPrice> prices) {
        syncPersistenceService.partitionDataAndSave(prices, priceRepository);
    }
}
