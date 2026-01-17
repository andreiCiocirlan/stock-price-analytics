package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.enums.NewHighLowMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.prices.ohlc.PriceWithPrevClose;
import stock.price.analytics.model.prices.ohlc.enums.CandleStickType;
import stock.price.analytics.model.stocks.Stock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptySet;
import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.INTRADAY_SPIKE_DOWN;
import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.INTRADAY_SPIKE_UP;
import static stock.price.analytics.util.Constants.INTRADAY_SPIKE_PERCENTAGE;

@Service
@RequiredArgsConstructor
class CacheServiceImpl implements CacheService {

    private final DailyPriceJsonCache dailyPriceJsonCache;
    private final StocksCache stocksCache;
    private final PricesCache pricesCache;
    private final HighLowPricesCache highLowPricesCache;
    private final PriceMilestoneCache priceMilestoneCache;
    private final CandleStickCache candleStickCache;


    @Override
    public List<DailyPriceJSON> dailyPriceJsonCache() {
        return dailyPriceJsonCache.getDailyPriceJSONByTicker().values().stream().toList();
    }

    @Override
    public List<DailyPriceJSON> addDailyPricesJSONAndReturn(List<DailyPriceJSON> dailyPriceJsons) {
        return dailyPriceJsonCache.cacheAndReturn(dailyPriceJsons);
    }

    @Override
    public List<DailyPrice> getCachedDailyPrices() {
        return pricesFor(StockTimeframe.DAILY).stream().map(p -> (DailyPrice) p).toList();
    }

    @Override
    public boolean weeklyHighLowDoesntExist() {
        return !highLowPricesCache.getWeeklyHighLowExists();
    }

    @Override
    public List<? extends HighLowForPeriod> highLowForPeriodPricesFor(HighLowPeriod period) {
        return highLowPricesCache.cacheForHighLowPeriod(period, false);
    }

    @Override
    public List<? extends HighLowForPeriod> prevWeekHighLowForPeriodPricesFor(HighLowPeriod period) {
        return highLowPricesCache.cacheForHighLowPeriod(period, true);
    }

    @Override
    public List<? extends HighLowForPeriod> highLowForPeriodPricesForNewHighLowMilestone(NewHighLowMilestone newHighLowMilestone) {
        return highLowPricesCache.cacheForNewHighLowMilestone(newHighLowMilestone);
    }

    @Override
    public List<? extends HighLowForPeriod> highLowForPeriodPricesForPricePerformanceMilestone(PricePerformanceMilestone pricePerformanceMilestone) {
        return highLowPricesCache.cacheForPricePerformanceMilestone(pricePerformanceMilestone);
    }

    @Override
    public List<? extends HighLowForPeriod> getUpdatedHighLowPricesForTickers(List<DailyPrice> dailyPrices, List<String> tickers, HighLowPeriod highLowPeriod) {
        return highLowPricesCache.getUpdatedHighLowPricesForTickers(dailyPrices, tickers, highLowPeriod);
    }

    @Override
    public List<String> getNewHighLowsForHLPeriod(HighLowPeriod highLowPeriod) {
        return new ArrayList<>(highLowPricesCache.getDailyNewHighLowsByHLPeriod().getOrDefault(highLowPeriod, emptySet()));
    }

    @Override
    public List<String> getEqualHighLowsForHLPeriod(HighLowPeriod highLowPeriod) {
        return new ArrayList<>(highLowPricesCache.getDailyEqualHighLowsByHLPeriod().getOrDefault(highLowPeriod, emptySet()));
    }

    @Override
    public void addHighLowPrices(List<? extends HighLowForPeriod> hlPricesUpdated, HighLowPeriod highLowPeriod) {
        highLowPricesCache.addHighLowPrices(hlPricesUpdated, highLowPeriod);
    }

    @Override
    public Map<String, Stock> getStocksMap() {
        return stocksCache.getStocksMap();
    }

    @Override
    public List<Stock> getCachedStocks() {
        return stocksCache.getCachedStocks();
    }

    @Override
    public List<String> getCachedTickers() {
        return stocksCache.getCachedTickers();
    }

    @Override
    public void addStocks(List<Stock> stocks) {
        stocksCache.addStocks(stocks);
    }

    @Override
    public List<AbstractPrice> pricesFor(StockTimeframe timeframe) {
        return new ArrayList<>(pricesCache.pricesFor(timeframe));
    }

    @Override
    public List<PriceWithPrevClose> pricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe) {
        return pricesCache.pricesWithPrevCloseFor(tickers, timeframe);
    }

    @Override
    public void addPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose, StockTimeframe timeframe) {
        pricesCache.addPricesWithPrevClose(pricesWithPrevClose, timeframe);
    }

    @Override
    public void cachePriceMilestoneTickers(PriceMilestone priceMilestone, List<String> tickers) {
        priceMilestoneCache.clearTickersByPriceMilestone(priceMilestone);
        priceMilestoneCache.cachePriceMilestoneTickers(priceMilestone, tickers);
    }

    @Override
    public List<String> tickersFor(PriceMilestone priceMilestone, List<Double> cfdMargins) {
        return getCachedStocks().stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .map(Stock::getTicker)
                .filter(priceMilestoneCache.tickersFor(priceMilestone)::contains).toList();
    }

    @Override
    public void updateIntradayPriceSpikesCache(List<DailyPrice> dailyPrices) {
        List<String> spikeUpTickers = new ArrayList<>();
        List<String> spikeDownTickers = new ArrayList<>();
        for (DailyPrice dailyPrice : dailyPrices) {
            String ticker = dailyPrice.getTicker();
            if (getStocksMap().get(ticker) != null) {
                double oldClosingPrice = getStocksMap().get(ticker).getClose();
                double newClosingPrice = dailyPrice.getClose();
                boolean spikeUp = newClosingPrice > oldClosingPrice * (1 + INTRADAY_SPIKE_PERCENTAGE);
                boolean spikeDown = newClosingPrice < oldClosingPrice * (1 - INTRADAY_SPIKE_PERCENTAGE);
                if (spikeUp) {
                    spikeUpTickers.add(ticker);
                } else if (spikeDown) {
                    spikeDownTickers.add(ticker);
                }
            }
        }
        cachePriceMilestoneTickers(INTRADAY_SPIKE_UP, spikeUpTickers);
        cachePriceMilestoneTickers(INTRADAY_SPIKE_DOWN, spikeDownTickers);
    }

    @Override
    public List<String> tickersFor(StockTimeframe timeframe, CandleStickType candleStickType) {
        return candleStickCache.tickersFor(timeframe, candleStickType);
    }

}
