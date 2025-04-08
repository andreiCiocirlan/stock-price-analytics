package stock.price.analytics.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.model.PriceWithPrevClose;
import stock.price.analytics.model.json.DailyPricesJSON;
import stock.price.analytics.model.prices.PriceMilestone;
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

    private final DailyPricesJSONCache dailyPricesJSONCache;
    private final DailyPricesCache dailyPricesCache;
    private final StocksCache stocksCache;
    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final HighLowPricesCache highLowPricesCache;
    private final PriceMilestoneCache priceMilestoneCache;
    @Getter @Setter
    private volatile long lastUpdateTimestamp = 0;


    public List<DailyPricesJSON> dailyPricesJSONCache() {
        return dailyPricesJSONCache.getDailyPricesJSONByTicker().values().stream().toList();
    }

    public List<DailyPricesJSON> cacheAndReturnDailyPricesJSON(List<DailyPricesJSON> dailyPricesJSON) {
        return dailyPricesJSONCache.addDailyPricesJSONInCacheAndReturn(dailyPricesJSON);
    }

    public boolean isFirstImportFor(StockTimeframe timeframe) {
        return dailyPricesCache.getFirstImportForTimeframe().get(timeframe);
    }

    public List<DailyPrice> cacheAndReturnDailyPrices(List<DailyPrice> dailyPrices) {
        return dailyPricesCache.addDailyPricesInCacheAndReturn(dailyPrices);
    }

    public void addPreMarketDailyPrices(List<DailyPrice> preMarketPrices) {
        dailyPricesCache.addDailyPrices(preMarketPrices, PRE);
    }

    public List<DailyPrice> getCachedDailyPrices(MarketState marketState) {
        return dailyPricesCache.dailyPrices(marketState);
    }

    public boolean weeklyHighLowExists() {
        return highLowPricesCache.getWeeklyHighLowExists();
    }

    public List<? extends HighLowForPeriod> highLowForPeriodPricesFor(HighLowPeriod period) {
        return highLowPricesCache.cacheForHighLowPeriod(period);
    }

    public List<? extends HighLowForPeriod> highLowForPeriodPricesForMilestone(PricePerformanceMilestone pricePerformanceMilestone) {
        return highLowPricesCache.cacheForMilestone(pricePerformanceMilestone);
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
        priceMilestoneCache.cachePriceMilestoneTickers(priceMilestone, tickers);
    }

    public Map<PriceMilestone, List<String>> tickersByPriceMilestones() {
        return priceMilestoneCache.tickersByPriceMilestones();
    }

    public void clearTickersByPriceMilestone() {
        priceMilestoneCache.clearTickersByPriceMilestone();
    }
}
