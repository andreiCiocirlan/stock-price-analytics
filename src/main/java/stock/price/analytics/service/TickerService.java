package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.client.YahooQuotesClient;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;
import stock.price.analytics.util.Constants;
import stock.price.analytics.util.JsonUtil;
import stock.price.analytics.util.TradingDateUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.Files.readAllLines;
import static stock.price.analytics.util.FileUtil.fileExistsFor;
import static stock.price.analytics.util.PricesUtil.getHigherTimeframePricesFor;
import static stock.price.analytics.util.PricesUtil.pricesWithPerformance;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickerService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final YahooQuotesClient yahooQuotesClient;
    private final StockService stockService;
    private final PriceService priceService;
    private final PriceGapService priceGapService;
    private final FairValueGapService fairValueGapService;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;
    private final HighLowForPeriodRepository highLowForPeriodRepository;

    @Transactional
    public void renameTicker(String oldTicker, String newTicker) {
        for (String table : Constants.DB_TABLES) {
            String column = "daily_prices_json".equals(table) ? "symbol" : "ticker";
            String sql = STR."UPDATE \{table} SET \{column} = ?2 WHERE \{column} = ?1";

            int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, oldTicker)
                    .setParameter(2, newTicker)
                    .executeUpdate();
            log.info("Ticker rename: updated {} rows for {} ", rowsAffected, table);
        }
    }

    @Transactional
    public void deleteAllDataFor(String ticker) {
        for (String table : Constants.DB_TABLES) {
            String column = "daily_prices_json".equals(table) ? "symbol" : "ticker";
            String sql = STR."DELETE FROM \{table} WHERE \{column} = ?1";

            int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, ticker)
                    .executeUpdate();
            log.info("deleted {} rows for {} ", rowsAffected, table);
        }
    }
    // import all data pertaining to the new tickers and create dailyPrices, htfPrices, stocks, highLowPrices etc.
    public void importAllDataFor(String tickers, Double cfdMargin, Boolean shortSell) {
        List<String> tickerList = Arrays.stream(tickers.split(",")).toList();
        List<String> newTickers = new ArrayList<>();
        List<String> tickersToCache = new ArrayList<>();
        for (String ticker : tickerList) {
            if (!cacheService.getCachedTickers().contains(ticker)) {
                tickersToCache.add(ticker);
            }
            if (!fileExistsFor(ticker)) {
                newTickers.add(ticker);
            }
        }

        LocalDate lastUpdate = stockService.findLastUpdate();
        if (!tickersToCache.isEmpty()) {
            cacheService.addStocks(tickersToCache.stream()
                    .map(ticker -> new Stock(ticker, Boolean.TRUE, shortSell, cfdMargin, lastUpdate))
                    .toList());
        }

        if (!newTickers.isEmpty()) { // call API to get the data and save the files
            yahooQuotesClient.getAllHistoricalPrices_andSaveJSONFileFor(String.join(",", newTickers));
        }
        List<DailyPrice> dailyPricesImported = getDailyPricesFor(tickerList);
        priceService.savePrices(dailyPricesImported);
        List<AbstractPrice> htfPricesImported = getHigherTimeframePricesFor(dailyPricesImported);
        priceService.savePrices(htfPricesImported);
        saveHighLowPricesForPeriodFrom(htfPricesImported);
        saveAndUpdateStocksFor(dailyPricesImported, htfPricesImported, lastUpdate);
        priceGapService.saveAllPriceGapsFor(tickerList);
        fairValueGapService.findNewFVGsAndSaveForAllTimeframes(tickerList, true);
    }

    private List<DailyPrice> getDailyPricesFor(List<String> tickerList) {
        List<DailyPrice> dailyPricesImported = new ArrayList<>();

        try {
            for (String ticker : tickerList) {
                String jsonFilePath = String.join("", "./all-historical-prices/DAILY/", ticker, ".json");
                String dailyPricesFileContent = String.join("", readAllLines(Path.of(jsonFilePath)));
                List<DailyPrice> dailyPricesExtracted = JsonUtil.extractDailyPricesFrom(ticker, dailyPricesFileContent).stream().sorted(Comparator.comparing(DailyPrice::getDate)).toList();
                dailyPricesImported.addAll(pricesWithPerformance(dailyPricesExtracted));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dailyPricesImported;
    }

    private void saveAndUpdateStocksFor(List<DailyPrice> dailyPricesImported, List<AbstractPrice> htfPricesImported, LocalDate lastUpdate) {
        List<DailyPrice> latestDailyPricesImported = dailyPricesImported.stream()
                .filter(dp -> dp.getDate().isEqual(lastUpdate))
                .toList();

        List<AbstractPrice> latestHTFPrices = htfPricesImported.stream()
                .filter(this::isLatestHTFPrice)
                .toList();

        stockService.updateStocksHighLowsAndOHLCFrom(latestDailyPricesImported, latestHTFPrices);
    }

    private boolean isLatestHTFPrice(AbstractPrice price) {
        LocalDate startDate = price.getStartDate();
        LocalDate tradingDateNow = TradingDateUtil.tradingDateNow();

        return switch (price.getTimeframe()) {
            case WEEKLY -> startDate.isEqual(tradingDateNow.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            case MONTHLY -> startDate.isEqual(tradingDateNow.with(TemporalAdjusters.firstDayOfMonth()));
            case QUARTERLY ->
                    startDate.isEqual(LocalDate.of(tradingDateNow.getYear(), tradingDateNow.getMonth().firstMonthOfQuarter().getValue(), 1));
            case YEARLY -> startDate.isEqual(tradingDateNow.with(TemporalAdjusters.firstDayOfYear()));
            default -> false;
        };
    }

    private void saveHighLowPricesForPeriodFrom(List<AbstractPrice> weeklyPricesImported) {
        Map<HighLowPeriod, List<HighLowForPeriod>> highLowForPeriodPrices = new HashMap<>();
        Map<String, List<AbstractPrice>> weeklyPricesByTicker = weeklyPricesImported.stream()
                .filter(price -> price.getTimeframe() == StockTimeframe.WEEKLY)
                .collect(Collectors.groupingBy(AbstractPrice::getTicker));

        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            int intervalNrWeeks = switch (highLowPeriod) {
                case HIGH_LOW_4W -> 4;
                case HIGH_LOW_52W -> 52;
                case HIGH_LOW_ALL_TIME -> 1;
            };
            for (Map.Entry<String, List<AbstractPrice>> entry : weeklyPricesByTicker.entrySet()) {
                String ticker = entry.getKey();
                List<AbstractPrice> prices = entry.getValue();
                prices.sort(Comparator.comparing(AbstractPrice::getStartDate).reversed());

                // NBIS is a newer ticker, for high_52w it had only 26 weeks of data
                if (prices.size() < intervalNrWeeks) {
                    intervalNrWeeks = 1;
                }
                for (int i = 0; i <= prices.size() - 1; i++) {
                    int toIndex = intervalNrWeeks == 1 ? prices.size() : Math.min(i + intervalNrWeeks, prices.size());
                    List<AbstractPrice> currentWeeksForPeriod = prices.subList(i, toIndex);
                    double highestPriceForPeriod = currentWeeksForPeriod.stream()
                            .mapToDouble(AbstractPrice::getHigh)
                            .max()
                            .orElseThrow();

                    double lowestPriceForPeriod = currentWeeksForPeriod.stream()
                            .mapToDouble(AbstractPrice::getLow)
                            .min()
                            .orElseThrow();

                    LocalDate startDate = prices.get(i).getStartDate();
                    LocalDate endDate = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

                    HighLowForPeriod highLowForPeriod = switch (highLowPeriod) {
                        case HIGH_LOW_4W ->
                                new HighLow4w(ticker, startDate, endDate, lowestPriceForPeriod, highestPriceForPeriod);
                        case HIGH_LOW_52W ->
                                new HighLow52Week(ticker, startDate, endDate, lowestPriceForPeriod, highestPriceForPeriod);
                        case HIGH_LOW_ALL_TIME ->
                                new HighestLowestPrices(ticker, startDate, endDate, lowestPriceForPeriod, highestPriceForPeriod);
                    };
                    highLowForPeriodPrices.computeIfAbsent(highLowPeriod, _ -> new ArrayList<>()).add(highLowForPeriod);
                }

            }
        }
        for (Map.Entry<HighLowPeriod, List<HighLowForPeriod>> entry : highLowForPeriodPrices.entrySet()) {
            List<HighLowForPeriod> highLowForPeriods = entry.getValue();
            HighLowPeriod highLowPeriod = entry.getKey();
            asyncPersistenceService.partitionDataAndSaveNoLogging(highLowForPeriods, highLowForPeriodRepository);
            cacheService.addHighLowPrices(highLowForPeriods, highLowPeriod);
        }
    }


}