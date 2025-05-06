package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.prices.ohlc.*;
import stock.price.analytics.util.QueryUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.temporal.TemporalAdjusters.*;
import static stock.price.analytics.util.PricesUtil.getHigherTimeframePricesFor;
import static stock.price.analytics.util.PricesUtil.multiplyWith;
import static stock.price.analytics.util.TradingDateUtil.isWithinSameTimeframe;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;


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
        List<WeeklyPrice> weeklyPricesToUpdate = weeklyPriceRepository.findByTickerAndDateLessThanEqual(ticker, splitDateWeeklyCutoff);
        List<MonthlyPrice> monthlyPricesToUpdate = monthlyPriceRepository.findByTickerAndDateLessThanEqual(ticker, splitDateMonthlyCutoff);
        List<QuarterlyPrice> quarterlyPricesToUpdate = quarterlyPriceRepository.findByTickerAndDateLessThanEqual(ticker, splitDateQuarterlyCutoff);
        List<YearlyPrice> yearlyPricesToUpdate = yearlyPriceRepository.findByTickerAndDateLessThanEqual(ticker, splitDateYearlyCutoff);

        // Update daily prices before stockSplitDate and keep others unchanged
        List<DailyPrice> updatedDailyPrices = dailyPricesToUpdate.stream()
                .map(dp -> dp.getDate().isBefore(stockSplitDate) ? (DailyPrice) multiplyWith(dp, priceMultiplier) : dp)
                .toList();

        // compute all higher timeframe prices using the updated daily prices
        List<AbstractPrice> htfPricesUpdated = getHigherTimeframePricesFor(updatedDailyPrices);

        Map<StockTimeframe, List<? extends AbstractPrice>> timeframeToDbPrices = Map.of(
                StockTimeframe.WEEKLY, weeklyPricesToUpdate,
                StockTimeframe.MONTHLY, monthlyPricesToUpdate,
                StockTimeframe.QUARTERLY, quarterlyPricesToUpdate,
                StockTimeframe.YEARLY, yearlyPricesToUpdate
        );

        Map<StockTimeframe, Map<String, AbstractPrice>> timeframeToDbPriceMap = timeframeToDbPrices.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .collect(Collectors.toMap(
                                        p -> p.getTicker() + "|" + p.getDate(),
                                        p -> (AbstractPrice) p
                                ))
                ));

        // update htf db prices
        for (AbstractPrice updatedPrice : htfPricesUpdated) {
            StockTimeframe timeframe = updatedPrice.getTimeframe();
            Map<String, AbstractPrice> dbPriceMap = timeframeToDbPriceMap.get(timeframe);
            if (dbPriceMap == null) continue;

            String key = updatedPrice.getTicker() + "|" + updatedPrice.getDate();
            AbstractPrice existingPrice = dbPriceMap.get(key);
            if (existingPrice != null) {
                existingPrice.setOpen(updatedPrice.getOpen());
                existingPrice.setHigh(updatedPrice.getHigh());
                existingPrice.setLow(updatedPrice.getLow());
                existingPrice.setClose(updatedPrice.getClose());
                existingPrice.setPerformance(updatedPrice.getPerformance());
            }
        }

        syncPersistenceService.partitionDataAndSave(updatedDailyPrices.stream().filter(price -> price.getDate().isBefore(stockSplitDate)).toList(), dailyPriceRepository);
        syncPersistenceService.partitionDataAndSave(weeklyPricesToUpdate, weeklyPriceRepository);
        syncPersistenceService.partitionDataAndSave(monthlyPricesToUpdate, monthlyPriceRepository);
        syncPersistenceService.partitionDataAndSave(quarterlyPricesToUpdate, quarterlyPriceRepository);
        syncPersistenceService.partitionDataAndSave(yearlyPricesToUpdate, yearlyPriceRepository);
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
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(AbstractPrice::getDate).reversed()).limit(3))
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
        String query = QueryUtil.checkImportStatusQueryFor(timeframe, checkFirstImport);
        Query nativeQuery = entityManager.createNativeQuery(query, Boolean.class);
        return (Boolean) nativeQuery.getResultList().getFirst();
    }

    public List<CandleWithDateDTO> findFor(String ticker, StockTimeframe timeframe) {
        String tableNameOHLC = timeframe.dbTableOHLC();
        String queryStr = STR."SELECT date, open, high, low, close FROM \{tableNameOHLC} WHERE ticker = :ticker ORDER BY date ASC";

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
            pricesUpdated.addAll(pricesWithPrevCloseUpdated.stream().map(PriceWithPrevClose::price).toList());
            cacheService.addPricesWithPrevClose(pricesWithPrevCloseUpdated, timeframe);
        }
        asyncPersistenceService.partitionDataAndSaveWithLogTime(pricesUpdated, priceRepository, "saved " + pricesUpdated.size() + " prices");
    }


    private List<PriceWithPrevClose> updateAndSavePrices(List<DailyPrice> importedDailyPrices,
                                                         StockTimeframe timeframe,
                                                         List<PriceWithPrevClose> pricesWithPrevClose) {
        List<PriceWithPrevClose> result = new ArrayList<>();
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevClose.stream()
                .collect(Collectors.toMap(priceWithPrevClose -> priceWithPrevClose.price().getTicker(), Function.identity()));
        for (DailyPrice importedDailyPrice : importedDailyPrices) {
            String ticker = importedDailyPrice.getTicker();
            PriceWithPrevClose priceWithPrevClose = pricesWithPrevCloseByTicker.get(ticker);
            AbstractPrice price = priceWithPrevClose.price();
            LocalDate latestDateWMQY = price.getDate(); // latest cached w,m,q,y date per ticker
            if (isWithinSameTimeframe(importedDailyPrice.getDate(), latestDateWMQY, timeframe)) {
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

    @Transactional
    public void savePrices(List<? extends AbstractPrice> prices) {
        syncPersistenceService.partitionDataAndSave(prices, priceRepository);
    }
}
