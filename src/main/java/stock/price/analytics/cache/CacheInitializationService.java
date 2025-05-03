package stock.price.analytics.cache;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.*;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.prices.ohlc.PriceWithPrevClose;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.json.DailyPriceJSONRepository;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.*;
import stock.price.analytics.util.CandleStickUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.highlow.enums.HighLowPeriod.HIGH_LOW_4W;
import static stock.price.analytics.util.Constants.NY_ZONE;
import static stock.price.analytics.util.LoggingUtil.logTime;
import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInitializationService {

    private final DailyPriceJsonCache dailyPriceJsonCache;
    private final StockRepository stockRepository;
    private final DailyPriceJSONRepository dailyPriceJSONRepository;
    private final PricesCache pricesCache;
    private final HighLowPricesCache highLowPricesCache;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private final StocksCache stocksCache;
    private final CacheService cacheService;
    private final SyncPersistenceService syncPersistenceService;
    private final StockService stockService;
    private final PriceService priceService;
    private final HighLowForPeriodService highLowForPeriodService;
    private final PriceMilestoneService priceMilestoneService;
    private final CandleStickCache candleStickCache;
    private final CandleStickService candleStickService;

    @Transactional
    public void initAllCaches() {
        initHighLowExists();

        List<Stock> stocks = stockRepository.findByXtbStockIsTrueAndDelistedDateIsNull();
        List<String> tickers = stocks.stream().map(Stock::getTicker).toList();
        logTime(() -> initPricesCache(tickers), "initialized prices cache");
        logTime(() -> initStocksCache(stocks), "initialized xtb stocks cache");
        logTime(this::initHighLowPricesCache, "initialized high low prices cache");
        logTime(this::initDailyJSONPricesCache, "initialized daily JSON prices cache");
        logTime(this::initPreMarketDailyPrices, "initialized pre-market daily prices cache");
        logTime(this::initTickersForPriceMilestoneCache, "initialized tickers for price milestone cache");
        logTime(this::initAvgCandleRangesCache, "initialized average candle range cache");
        logTime(this::initCandleStickTypeCache, "initialized candle stick types cache");
    }

    private void initTickersForPriceMilestoneCache() {
        priceMilestoneService.cacheTickersForMilestones();
    }

    private void initHighLowExists() {
        boolean weeklyHighLowExists = highLowForPeriodService.weeklyHighLowExists();
        log.info("initialized weeklyHighLowExists {}", weeklyHighLowExists);
        highLowPricesCache.setWeeklyHighLowExists(weeklyHighLowExists);
    }

    private void initDailyJSONPricesCache() {
        LocalDate tradingDateNow = tradingDateNow();
        List<DailyPriceJSON> dailyPriceJSONs = dailyPriceJSONRepository.findByDateBetween(tradingDateNow.minusDays(7), tradingDateNow);
        dailyPriceJsonCache.cacheAndReturn(dailyPriceJSONs.stream()
                .collect(Collectors.groupingBy(DailyPriceJSON::getSymbol))
                .values().stream()
                .flatMap(prices -> prices.stream()
                        .sorted(Comparator.comparing(DailyPriceJSON::getDate).reversed())
                        .limit(2))
                .toList());
    }

    private void initPreMarketDailyPrices() {
        Map<String, List<DailyPriceJSON>> dailyPriceJSONsByTicker = dailyPriceJsonCache.getDailyPriceJSONByTicker().values().stream()
                .sorted(Comparator.comparing(DailyPriceJSON::getDate).reversed()) // order by date desc
                .collect(Collectors.groupingBy(DailyPriceJSON::getSymbol));

        List<DailyPrice> latestPreMarketDailyPrices = new ArrayList<>();

        for (List<DailyPriceJSON> dailyPriceJSONs : dailyPriceJSONsByTicker.values()) {
            DailyPriceJSON latestPrice = dailyPriceJSONs.getFirst(); // take the first (latest) daily price per ticker
            if (latestPrice.getPreMarketPrice() != 0d) {
                latestPreMarketDailyPrices.add(latestPrice.convertToDailyPrice(true));
            }
        }
        pricesCache.addPreMarketPrices(latestPreMarketDailyPrices);
    }

    private void initHighLowPricesCache() {
        LocalDate latestDailyPriceImportDate = stockService.findLastUpdate();
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            initHighLowPriceCache(highLowPeriod, latestDailyPriceImportDate);
            initPrevWeekHighLowPricesCache(highLowPeriod);
        }
        if (!cacheService.weeklyHighLowExists()) {
            stockService.updateHighLowForPeriodFromHLCachesAndAdjustWeekend();
            highLowPricesCache.setWeeklyHighLowExists(true);
        }
    }

    private void initPrevWeekHighLowPricesCache(HighLowPeriod highLowPeriod) {
        LocalDate prevWeekStartDate = LocalDate.now(NY_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        log.info("prevWeekStartDate " + prevWeekStartDate);
        List<? extends HighLowForPeriod> prevWeekHighLowPrices = highLowForPeriodService.hlPricesForDate(highLowPeriod, prevWeekStartDate);
        highLowPricesCache.addPrevWeekHighLowPrices(prevWeekHighLowPrices, highLowPeriod);
    }

    private void initHighLowPriceCache(HighLowPeriod highLowPeriod, LocalDate latestDailyPriceImportDate) {
        LocalDate startDate = latestDailyPriceImportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!cacheService.weeklyHighLowExists()) { // on first import of the week need to find min/max prices for the past 3 weeks and 51 weeks respectively (new objects)
            LocalDate newWeekStartDate = startDate.plusWeeks(1);
            if (highLowPeriod == HighLowPeriod.HIGH_LOW_ALL_TIME) { // for all-time highs/lows simply copy the existing row on Mondays
                List<HighestLowestPrices> highestLowestPrices = new ArrayList<>();
                highLowForPeriodService.hlPricesForDate(highLowPeriod, startDate).forEach(hlp -> highestLowestPrices.add(((HighestLowestPrices) hlp).copyWith(newWeekStartDate)));
                syncPersistenceService.partitionDataAndSave(highestLowestPrices, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(highestLowestPrices, highLowPeriod);
            } else { // for 4w, 52w need sql select for the period (for all-time it would simply be a copy)
                int nrWeeksLookBack = highLowPeriod == HIGH_LOW_4W ? 3 : 51; // new week -> look back 3, 51 instead of 4, 52 weeks
                List<HighLowForPeriod> highLowForPeriods = highLowForPeriodRepository.highLowPricesInPastWeeks(startDate, nrWeeksLookBack)
                        .stream()
                        .map(dto -> convertToHighLowForPeriod(dto, newWeekStartDate, highLowPeriod))
                        .toList();
                syncPersistenceService.partitionDataAndSave(highLowForPeriods, highLowForPeriodRepository);
                highLowPricesCache.addHighLowPrices(highLowForPeriods, highLowPeriod);
            }
        } else {
            List<? extends HighLowForPeriod> highLowPrices = highLowForPeriodService.hlPricesForDate(highLowPeriod, startDate);
            highLowPricesCache.addHighLowPrices(highLowPrices, highLowPeriod);
        }
    }

    private HighLowForPeriod convertToHighLowForPeriod(TickerHighLowView dto, LocalDate startDate, HighLowPeriod highLowPeriod) {
        return switch (highLowPeriod) {
            case HIGH_LOW_4W -> new HighLow4w(dto.getTicker(), startDate, startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)), dto.getLow(), dto.getHigh());
            case HIGH_LOW_52W -> new HighLow52Week(dto.getTicker(), startDate, startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)), dto.getLow(), dto.getHigh());
            case HIGH_LOW_ALL_TIME -> throw new IllegalArgumentException("HIGH_LOW_ALL_TIME is not supported.");
        };
    }

    private void initStocksCache(List<Stock> stocks) {
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
            delistedStocks.forEach(delisted -> stocksCache.getStocksMap().remove(delisted.getTicker()));
            syncPersistenceService.partitionDataAndSave(delistedStocks, stockRepository);
        }
    }

    private void initPricesCache(List<String> tickers) {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            addPricesWithPrevCloseFrom(priceService.previousThreePricesFor(tickers, timeframe));
        }
    }

    private void addPricesWithPrevCloseFrom(List<? extends AbstractPrice> prevThreePrices) {
        List<PriceWithPrevClose> pricesWithPrevClose = pricesWithPrevCloseByTickerFrom(prevThreePrices);
        pricesCache.addPricesWithPrevClose(pricesWithPrevClose, prevThreePrices.getFirst().getTimeframe());
    }

    private List<PriceWithPrevClose> pricesWithPrevCloseByTickerFrom(List<? extends AbstractPrice> previousThreePricesForTickers) {
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
                .map(price -> new PriceWithPrevClose(price, previousCloseByTicker.get(price.getTicker())))
                .toList();
    }

    private void initAvgCandleRangesCache() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            candleStickService.averageCandleRangesFor(timeframe)
                    .forEach((ticker, range) -> candleStickCache.addAvgCandleRangeFor(ticker, timeframe, range));
        }
    }

    private void initCandleStickTypeCache() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            List<AbstractPrice> pricesForTimeframe = cacheService.pricesFor(timeframe);
            for (AbstractPrice price : pricesForTimeframe) {
                String ticker = price.getTicker();
                CandleStickType candleStickType = price.toCandleStickType();
                if (CandleStickUtil.isTightCandleStick(price, cacheService.averageCandleRangeFor(timeframe, price.getTicker()))) {
                    candleStickType = CandleStickType.TIGHT;
                }
                candleStickCache.addTickerFor(candleStickType, timeframe, ticker);
            }
        }
    }
}
