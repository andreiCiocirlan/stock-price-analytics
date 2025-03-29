package stock.price.analytics.cache;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.model.*;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.*;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.json.DailyPricesJSON;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;
import stock.price.analytics.repository.prices.json.DailyPricesJSONRepository;
import stock.price.analytics.repository.prices.ohlc.DailyPricesRepository;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.AsyncPersistenceService;
import stock.price.analytics.service.HighLowForPeriodService;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.StockService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInitializationService {

    private final DailyPricesJSONCache dailyPricesJSONCache;
    private final DailyPricesCache dailyPricesCache;
    private final StockRepository stockRepository;
    private final DailyPricesJSONRepository dailyPricesJSONRepository;
    private final DailyPricesRepository dailyPricesRepository;
    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final HighLowPricesCache highLowPricesCache;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private final StocksCache stocksCache;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;
    private final StockService stockService;
    private final PricesService pricesService;
    private final HighLowForPeriodService highLowForPeriodService;

    @Transactional
    public void initializeAllCaches() {
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            boolean firstImportFor = pricesService.isFirstImportFor(timeframe);
            System.out.println(timeframe + " isFirstImport: " + firstImportFor);
            setFirstImportFor(timeframe, firstImportFor);
        }

        List<Stock> stocks = stockRepository.findByXtbStockIsTrueAndDelistedDateIsNull();
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            logTime(() -> initHigherTimeframePricesCache(pricesService.previousThreePricesFor(tickers, timeframe)), "initialized " + timeframe + " prices cache");
        }
        logTime(() -> initializeStocks(stocks), "initialized xtb stocks cache");
        LocalDate latestDailyPriceImportDate = stockService.findLastUpdate(); // find last update from stocksCache
        boolean weeklyHighLowExists = highLowForPeriodService.weeklyHighLowExists();
        logTime(() -> initHighLowPricesCache(latestDailyPriceImportDate, weeklyHighLowExists), "initialized high low prices cache");
        if (!weeklyHighLowExists && cacheService.isFirstImportFor(StockTimeframe.WEEKLY)) {
            stockService.updateHighLowForPeriodFromHLCachesAndAdjustWeekend();
        }
        logTime(this::initLatestDailyPricesCache, "initialized latest daily prices cache");
        logTime(this::initDailyJSONPricesCache, "initialized daily JSON prices cache");
        logTime(this::initializePreMarketDailyPrices, "initialized pre-market daily prices cache");
    }

    private void initDailyJSONPricesCache() {
        LocalDate tradingDateNow = tradingDateNow();
        dailyPricesJSONCache.addDailyJSONPrices(dailyPricesJSONRepository.findByDateBetween(tradingDateNow.minusDays(7), tradingDateNow));
    }

    private void initializePreMarketDailyPrices() {
        Map<String, List<DailyPricesJSON>> dailyPricesJSONByTicker = dailyPricesJSONCache.getDailyPricesJSONByTicker().values().stream()
                .sorted(Comparator.comparing(DailyPricesJSON::getDate).reversed()) // order by date desc
                .collect(Collectors.groupingBy(DailyPricesJSON::getSymbol));

        List<DailyPrice> latestPreMarketDailyPrices = new ArrayList<>();

        for (List<DailyPricesJSON> dailyPricesJSONs : dailyPricesJSONByTicker.values()) {
            DailyPricesJSON latestPrice = dailyPricesJSONs.getFirst(); // take the first (latest) daily price per ticker
            if (latestPrice.getPreMarketPrice() != 0d) {
                latestPreMarketDailyPrices.add(latestPrice.convertToDailyPrice(true));
            }
        }
        dailyPricesCache.addDailyPrices(latestPreMarketDailyPrices, PRE);
    }

    private void initLatestDailyPricesCache() {
        dailyPricesCache.addDailyPrices(dailyPricesRepository.findLatestDailyPrices(), REGULAR);
    }

    private void setFirstImportFor(StockTimeframe timeframe, Boolean isFirstImport) {
        dailyPricesCache.getFirstImportForTimeframe().put(timeframe, isFirstImport);
    }

    private void initHighLowPricesCache(LocalDate latestDailyPriceImportDate, boolean weeklyHighLowExists) {
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            initHighLowPriceCache(highLowPeriod, latestDailyPriceImportDate, weeklyHighLowExists);
            initPrevWeekHighLowPricesCache(highLowPeriod, latestDailyPriceImportDate, weeklyHighLowExists);
        }
    }

    private void initPrevWeekHighLowPricesCache(HighLowPeriod highLowPeriod, LocalDate latestDailyPriceImportDate, boolean weeklyHighLowExists) {
        LocalDate prevWeekStartDate = latestDailyPriceImportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (weeklyHighLowExists && !cacheService.isFirstImportFor(StockTimeframe.WEEKLY)) { // on first import of the week need to find min/max prices for the past 3 weeks and 51 weeks respectively (new objects)
            prevWeekStartDate = prevWeekStartDate.minusWeeks(1);
        }
        List<? extends HighLowForPeriod> prevWeekHighLowPrices = switch (highLowPeriod) {
            case HIGH_LOW_4W -> highLowForPeriodRepository.highLow4wPricesFor(prevWeekStartDate);
            case HIGH_LOW_52W -> highLowForPeriodRepository.highLow52wPricesFor(prevWeekStartDate);
            case HIGH_LOW_ALL_TIME -> highLowForPeriodRepository.highestLowestPrices(prevWeekStartDate);
        };
        highLowPricesCache.addPrevWeekHighLowPrices(prevWeekHighLowPrices, highLowPeriod);
    }

    private void initHighLowPriceCache(HighLowPeriod highLowPeriod, LocalDate latestDailyPriceImportDate, boolean weeklyHighLowExists) {
        LocalDate startDate = latestDailyPriceImportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = latestDailyPriceImportDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        if (!weeklyHighLowExists && cacheService.isFirstImportFor(StockTimeframe.WEEKLY)) { // on first import of the week need to find min/max prices for the past 3 weeks and 51 weeks respectively (new objects)
            LocalDate newWeekStartDate = startDate.plusWeeks(1);
            LocalDate newWeekEndDate = endDate.plusWeeks(1);
            if (highLowPeriod == HighLowPeriod.HIGH_LOW_ALL_TIME) { // for all-time highs/lows simply copy the existing row on Mondays
                List<HighestLowestPrices> highestLowestPrices = new ArrayList<>();
                highLowForPeriodRepository.highestLowestPrices(startDate).forEach(hlp -> highestLowestPrices.add(hlp.copyWith(newWeekStartDate)));
                asyncPersistenceService.partitionDataAndSave(highestLowestPrices, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(highestLowestPrices, highLowPeriod);
            } else { // for 4w, 52w need sql select for the period (for all-time it would simply be a copy)
                int weekCount = switch (highLowPeriod) {
                    case HIGH_LOW_4W -> 3; // last imported date was Friday -> new week -> look back 3 instead of 4 weeks
                    case HIGH_LOW_52W -> 51;
                    case HIGH_LOW_ALL_TIME -> throw new IllegalArgumentException("HIGH_LOW_ALL_TIME is not supported.");
                };
                List<HighLowForPeriod> highLowForPeriods = highLowForPeriodRepository.highLowPricesInPastWeeks(startDate, weekCount)
                        .stream()
                        .map(dto -> convertToHighLowForPeriod(dto, newWeekStartDate, newWeekEndDate, highLowPeriod))
                        .toList();
                asyncPersistenceService.partitionDataAndSave(highLowForPeriods, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(highLowForPeriods, highLowPeriod);
            }
        } else {
            List<? extends HighLowForPeriod> highLowPrices = switch (highLowPeriod) {
                case HIGH_LOW_4W -> highLowForPeriodRepository.highLow4wPricesFor(startDate);
                case HIGH_LOW_52W -> highLowForPeriodRepository.highLow52wPricesFor(startDate);
                case HIGH_LOW_ALL_TIME -> highLowForPeriodRepository.highestLowestPrices(startDate);
            };
            highLowPricesCache.addHighLowPrices(highLowPrices, highLowPeriod);
        }
    }

    private HighLowForPeriod convertToHighLowForPeriod(TickerHighLowView dto, LocalDate newWeekStartDate, LocalDate newWeekEndDate, HighLowPeriod highLowPeriod) {
        HighLowForPeriod highLowForPeriod = switch (highLowPeriod) {
            case HIGH_LOW_4W -> new HighLow4w(dto.getTicker(), newWeekStartDate, newWeekEndDate);
            case HIGH_LOW_52W -> new HighLow52Week(dto.getTicker(), newWeekStartDate, newWeekEndDate);
            case HIGH_LOW_ALL_TIME -> throw new IllegalArgumentException("HIGH_LOW_ALL_TIME is not supported.");
        };
        highLowForPeriod.setLow(dto.getLow());
        highLowForPeriod.setHigh(dto.getHigh());
        return highLowForPeriod;
    }

    private void initializeStocks(List<Stock> stocks) {
        stocksCache.addStocks(stocks);
        findDelistedStocksAndUpdate();
    }

    private void findDelistedStocksAndUpdate() {
        List<Stock> delistedStocks = stocksCache.getStocksMap().values().stream()
                .filter(stock -> stock.getLastUpdated().isBefore(LocalDate.now().minusDays(5)))
                .peek(stock -> {
                    log.warn("DELISTED stock {}", stock.getTicker());
                    stock.setDelistedDate(stock.getLastUpdated());
                })
                .toList();

        if (!delistedStocks.isEmpty()) {
            asyncPersistenceService.partitionDataAndSave(delistedStocks, stockRepository);
        }
    }

    private void initHigherTimeframePricesCache(List<AbstractPrice> previousThreePrices) {
        addHtfPricesWithPrevCloseFrom(previousThreePrices);
    }

    private void addHtfPricesWithPrevCloseFrom(List<AbstractPrice> prevThreePrices) {
        List<PriceWithPrevClose> pricesWithPrevClose = pricesWithPrevCloseByTickerFrom(prevThreePrices);
        higherTimeframePricesCache.addPricesWithPrevClose(pricesWithPrevClose, prevThreePrices.getFirst().getTimeframe());
    }

    private List<PriceWithPrevClose> pricesWithPrevCloseByTickerFrom(List<stock.price.analytics.model.prices.ohlc.AbstractPrice> previousThreePricesForTickers) {
        Map<String, List<AbstractPrice>> previousTwoPricesByTicker = previousThreePricesForTickers
                .stream()
                .collect(Collectors.groupingBy(AbstractPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(AbstractPrice::getStartDate).reversed()).limit(2))
                .collect(Collectors.groupingBy(AbstractPrice::getTicker));
        List<stock.price.analytics.model.prices.ohlc.AbstractPrice> latestPrices = new ArrayList<>();
        Map<String, Double> previousCloseByTicker = new HashMap<>();
        for (List<stock.price.analytics.model.prices.ohlc.AbstractPrice> prices : previousTwoPricesByTicker.values()) {
            latestPrices.add(prices.get(0)); // most recent price
            previousCloseByTicker.put(prices.get(0).getTicker(),
                    prices.size() > 1 ? prices.get(1).getClose() : prices.get(0).getOpen()); // if IPO week, month, quarter, year -> take opening price
        }

        return latestPrices.stream()
                .map(price -> (PriceWithPrevClose) switch (price.getTimeframe()) {
                    case DAILY -> throw new IllegalStateException("Unexpected timeframe DAILY");
                    case WEEKLY -> new WeeklyPriceWithPrevClose((WeeklyPrice) price, previousCloseByTicker.get(price.getTicker()));
                    case MONTHLY -> new MonthlyPriceWithPrevClose((MonthlyPrice) price, previousCloseByTicker.get(price.getTicker()));
                    case QUARTERLY -> new QuarterlyPriceWithPrevClose((QuarterlyPrice) price, previousCloseByTicker.get(price.getTicker()));
                    case YEARLY -> new YearlyPriceWithPrevClose((YearlyPrice) price, previousCloseByTicker.get(price.getTicker()));
                })
                .toList();
    }
}
