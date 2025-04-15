package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.model.PriceWithPrevClose;
import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.enums.NewHighLowMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptySet;
import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final DailyPriceJsonCache dailyPriceJsonCache;
    private final DailyPriceCache dailyPriceCache;
    private final StocksCache stocksCache;
    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final HighLowPricesCache highLowPricesCache;
    private final PriceMilestoneCache priceMilestoneCache;


    public List<DailyPriceJSON> dailyPriceJsonCache() {
        return dailyPriceJsonCache.getDailyPriceJSONByTicker().values().stream().toList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> cacheAndReturn(List<T> entities) {
        if (entities.isEmpty()) {
            return entities;
        }

        return switch (entities.getFirst()) {
            case DailyPrice _ -> (List<T>) dailyPriceCache.cacheAndReturn((List<DailyPrice>) entities);
            case DailyPriceJSON _ -> (List<T>) dailyPriceJsonCache.cacheAndReturn((List<DailyPriceJSON>) entities);
            default -> throw new IllegalArgumentException("Unsupported type: " + entities.getFirst().getClass());
        };
    }

    public boolean isFirstImportFor(StockTimeframe timeframe) {
        return dailyPriceCache.getFirstImportForTimeframe().get(timeframe);
    }

    public void addPreMarketDailyPrices(List<DailyPrice> preMarketPrices) {
        dailyPriceCache.addDailyPrices(preMarketPrices, PRE);
    }

    public List<DailyPrice> getCachedDailyPrices(MarketState marketState) {
        return dailyPriceCache.dailyPrices(marketState);
    }

    public boolean weeklyHighLowExists() {
        return highLowPricesCache.getWeeklyHighLowExists();
    }

    public List<? extends HighLowForPeriod> highLowForPeriodPricesFor(HighLowPeriod period) {
        return highLowPricesCache.cacheForHighLowPeriod(period, false);
    }

    public List<? extends HighLowForPeriod> prevWeekHighLowForPeriodPricesFor(HighLowPeriod period) {
        return highLowPricesCache.cacheForHighLowPeriod(period, true);
    }

    public List<? extends HighLowForPeriod> highLowForPeriodPricesForNewHighLowMilestone(NewHighLowMilestone newHighLowMilestone) {
        return highLowPricesCache.cacheForNewHighLowMilestone(newHighLowMilestone);
    }

    public List<? extends HighLowForPeriod> highLowForPeriodPricesForPricePerformanceMilestone(PricePerformanceMilestone pricePerformanceMilestone) {
        return highLowPricesCache.cacheForPricePerformanceMilestone(pricePerformanceMilestone);
    }

    public List<? extends HighLowForPeriod> getUpdatedHighLowPricesForTickers(List<DailyPrice> dailyPrices, List<String> tickers, HighLowPeriod highLowPeriod) {
        return highLowPricesCache.getUpdatedHighLowPricesForTickers(dailyPrices, tickers, highLowPeriod);
    }

    public List<String> getNewHighLowsForHLPeriod(HighLowPeriod highLowPeriod) {
        return new ArrayList<>(highLowPricesCache.getDailyNewHighLowsByHLPeriod().getOrDefault(highLowPeriod, emptySet()));
    }

    public List<String> getEqualHighLowsForHLPeriod(HighLowPeriod highLowPeriod) {
        return new ArrayList<>(highLowPricesCache.getDailyEqualHighLowsByHLPeriod().getOrDefault(highLowPeriod, emptySet()));
    }

    public void addHighLowPrices(List<? extends HighLowForPeriod> hlPricesUpdated, HighLowPeriod highLowPeriod) {
        highLowPricesCache.addHighLowPrices(hlPricesUpdated, highLowPeriod);
    }

    public Map<String, Stock> getStocksMap() {
        return stocksCache.getStocksMap();
    }

    public List<Stock> getCachedStocks() {
        return stocksCache.getCachedStocks();
    }

    public List<String> getCachedTickers() {
        return stocksCache.getCachedTickers();
    }

    public void addStocks(List<Stock> stocks) {
        stocksCache.addStocks(stocks);
    }

    public List<AbstractPrice> htfPricesFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> new ArrayList<>(getCachedDailyPrices(REGULAR));
            case WEEKLY, MONTHLY, QUARTERLY, YEARLY -> new ArrayList<>(higherTimeframePricesCache.htfPricesFor(timeframe));
        };
    }

    public List<PriceWithPrevClose> htfPricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe) {
        return higherTimeframePricesCache.pricesWithPrevCloseFor(tickers, timeframe);
    }

    public void addHtfPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose) {
        higherTimeframePricesCache.addPricesWithPrevClose(pricesWithPrevClose, pricesWithPrevClose.getFirst().getPrice().getTimeframe());
    }

    public void logNewHighLowsForHLPeriods() {
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            List<String> newHighLowsForHLPeriod = getNewHighLowsForHLPeriod(highLowPeriod);
            if (!newHighLowsForHLPeriod.isEmpty()) {
                log.info("{} New {} : {}", newHighLowsForHLPeriod.size(), highLowPeriod, newHighLowsForHLPeriod);
            }
        }
    }

    public void logEqualHighLowsForHLPeriods() {
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            List<String> equalHighLowsForHLPeriod = getEqualHighLowsForHLPeriod(highLowPeriod);
            if (!equalHighLowsForHLPeriod.isEmpty()) {
                log.info("{} Equal {} : {}", equalHighLowsForHLPeriod.size(), highLowPeriod, equalHighLowsForHLPeriod);
            }
        }
    }

    public void cachePriceMilestoneTickers(PriceMilestone priceMilestone, List<String> tickers) {
        priceMilestoneCache.clearTickersByPriceMilestone(priceMilestone);
        priceMilestoneCache.cachePriceMilestoneTickers(priceMilestone, tickers);
    }

    public Map<PriceMilestone, List<String>> tickersByPriceMilestones() {
        return priceMilestoneCache.tickersByPriceMilestones();
    }

    public List<String> tickersFor(PriceMilestone priceMilestone, List<Double> cfdMargins) {
        return getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .map(Stock::getTicker)
                .filter(priceMilestoneCache.tickersFor(priceMilestone)::contains).toList();
    }

}
