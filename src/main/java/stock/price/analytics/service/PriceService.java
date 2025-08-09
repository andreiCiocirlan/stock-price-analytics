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
import stock.price.analytics.util.query.importstatus.ImportStatusQueryProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.time.temporal.TemporalAdjusters.*;
import static stock.price.analytics.util.DateUtil.convertDateToTimeframe;
import static stock.price.analytics.util.PricesUtil.getHigherTimeframePricesMapFor;
import static stock.price.analytics.util.PricesUtil.multiplyWith;
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
    private final ImportStatusQueryProvider importStatusQueryProvider;

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
        List<AbstractPrice> htfPricesUpdated = new ArrayList<>();
        for (Map.Entry<StockTimeframe, List<AbstractPrice>> entry : getHigherTimeframePricesMapFor(updatedDailyPrices).entrySet()) {
            htfPricesUpdated.addAll(entry.getValue());
        }

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
            case DAILY -> findPreviousSevenDailyPricesForTickers(tickers);
            case WEEKLY -> weeklyPriceRepository.findPreviousThreeWeeklyPricesForTickers(tickers);
            case MONTHLY -> monthlyPriceRepository.findPreviousThreeMonthlyPricesForTickers(tickers);
            case QUARTERLY -> quarterlyPriceRepository.findPreviousThreeQuarterlyPricesForTickers(tickers);
            case YEARLY -> yearlyPriceRepository.findPreviousThreeYearlyPricesForTickers(tickers);
        };
    }

    private List<? extends AbstractPrice> findPreviousSevenDailyPricesForTickers(List<String> tickers) {
        List<DailyPrice> previousSevenDailyPricesForTickers = dailyPriceRepository.findDailyPricesForTickersFromLastWeekToDate(tickers);

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
        String query = importStatusQueryProvider.checkImportStatusQueryFor(timeframe, checkFirstImport);
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

    private List<String> compositeIdsFromDailyPricesForTimeframe(List<DailyPrice> dailyPrices, StockTimeframe timeframe) {
        return dailyPrices.stream()
                .map(dp -> dp.getTicker() + "_" +
                           convertDateToTimeframe(dp.getDate(), timeframe).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .distinct()
                .toList();
    }

    @Transactional
    public void updateAllTimeframePrices(List<DailyPrice> importedDailyPrices) {
        // Update prices for each timeframe
        List<AbstractPrice> pricesUpdated = new ArrayList<>();
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            List<String> compositeIdsForTimeframe = compositeIdsFromDailyPricesForTimeframe(importedDailyPrices, timeframe);
            List<PriceWithPrevClose> pricesWithPrevCloseUpdated = updateAndSavePrices(importedDailyPrices, timeframe,
                    cacheService.pricesWithPrevCloseFor(compositeIdsForTimeframe, timeframe));
            pricesUpdated.addAll(pricesWithPrevCloseUpdated.stream().map(PriceWithPrevClose::price).toList());
            cacheService.addPricesWithPrevClose(pricesWithPrevCloseUpdated, timeframe);
        }
        asyncPersistenceService.partitionDataAndSaveWithLogTime(pricesUpdated, priceRepository, "saved " + pricesUpdated.size() + " prices");
    }


    private List<PriceWithPrevClose> updateAndSavePrices(List<DailyPrice> importedDailyPrices,
                                                         StockTimeframe timeframe,
                                                         List<PriceWithPrevClose> cachedPricesWithPrevClose) {
        List<PriceWithPrevClose> result = new ArrayList<>();
        Map<String, PriceWithPrevClose> cachedPricesByCompositeId = cachedPricesWithPrevClose.stream()
                .collect(Collectors.toMap(p -> p.price().getCompositeId(), Function.identity()));
        for (DailyPrice importedDailyPrice : importedDailyPrices) {
            LocalDate periodDate = convertDateToTimeframe(importedDailyPrice.getDate(), timeframe);
            String compositeId = importedDailyPrice.getTicker() + "_" + periodDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

            PriceWithPrevClose priceWithPrevClose = cachedPricesByCompositeId.get(compositeId);
            if (priceWithPrevClose != null) {
                AbstractPrice price = priceWithPrevClose.price();
                LocalDate latestDateWMQY = price.getDate(); // latest cached w,m,q,y date per ticker

                if (isWithinSameTimeframe(importedDailyPrice.getDate(), latestDateWMQY, timeframe)) {
                    price.convertFrom(importedDailyPrice, priceWithPrevClose.previousClose());
                    result.add(priceWithPrevClose);
                } else {
                    // New timeframe period: create new PriceWithPrevClose object
                    result.add(newPriceWithPrevCloseFrom(importedDailyPrice, timeframe, price.getClose()));
                }
            } else {
                // No cached price for this compositeId, so create a new one
                result.add(newPriceWithPrevCloseFrom(importedDailyPrice, timeframe, importedDailyPrice.getOpen()));
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

    @Transactional
    public void updatePrices(List<? extends AbstractPrice> prices, StockTimeframe timeframe) {
        String tableName = timeframe.dbTableOHLC();

        int batchSize = 250;
        for (int i = 0; i < prices.size(); i++) {
            AbstractPrice price = prices.get(i);
            int updatedCount = entityManager.createNativeQuery(
                            STR."""
                                UPDATE \{tableName}
                                SET open = :open, high = :high, low = :low, close = :close
                                WHERE date = :date AND ticker = :ticker
                            """)
                    .setParameter("open", price.getOpen())
                    .setParameter("high", price.getHigh())
                    .setParameter("low", price.getLow())
                    .setParameter("close", price.getClose())
                    .setParameter("date", price.getDate())
                    .setParameter("ticker", price.getTicker())
                    .executeUpdate();

            if (updatedCount != 0) {
                log.info("updated {} {} rows", updatedCount, tableName);
            }

            if (i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

}
