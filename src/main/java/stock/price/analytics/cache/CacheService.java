package stock.price.analytics.cache;

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

import java.util.List;
import java.util.Map;

public interface CacheService {
    List<DailyPriceJSON> dailyPriceJsonCache();

    List<DailyPriceJSON> addDailyPricesJSONAndReturn(List<DailyPriceJSON> dailyPriceJsons);

    List<DailyPrice> getCachedDailyPrices();

    boolean weeklyHighLowDoesntExist();

    List<? extends HighLowForPeriod> highLowForPeriodPricesFor(HighLowPeriod period);

    List<? extends HighLowForPeriod> prevWeekHighLowForPeriodPricesFor(HighLowPeriod period);

    List<? extends HighLowForPeriod> highLowForPeriodPricesForNewHighLowMilestone(NewHighLowMilestone newHighLowMilestone);

    List<? extends HighLowForPeriod> highLowForPeriodPricesForPricePerformanceMilestone(PricePerformanceMilestone pricePerformanceMilestone);

    List<? extends HighLowForPeriod> getUpdatedHighLowPricesForTickers(List<DailyPrice> dailyPrices, List<String> tickers, HighLowPeriod highLowPeriod);

    List<String> getNewHighLowsForHLPeriod(HighLowPeriod highLowPeriod);

    List<String> getEqualHighLowsForHLPeriod(HighLowPeriod highLowPeriod);

    void addHighLowPrices(List<? extends HighLowForPeriod> hlPricesUpdated, HighLowPeriod highLowPeriod);

    Map<String, Stock> getStocksMap();

    List<Stock> getCachedStocks();

    List<String> getCachedTickers();

    void addStocks(List<Stock> stocks);

    List<AbstractPrice> pricesFor(StockTimeframe timeframe);

    List<PriceWithPrevClose> pricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe);

    void addPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose, StockTimeframe timeframe);

    void cachePriceMilestoneTickers(PriceMilestone priceMilestone, List<String> tickers);

    List<String> tickersFor(PriceMilestone priceMilestone, List<Double> cfdMargins);

    void updateIntradayPriceSpikesCache(List<DailyPrice> dailyPrices);

    List<String> tickersFor(StockTimeframe timeframe, CandleStickType candleStickType);
}
