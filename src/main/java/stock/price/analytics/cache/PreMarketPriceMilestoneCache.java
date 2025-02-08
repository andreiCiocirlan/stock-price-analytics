package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;

@Component
@RequiredArgsConstructor
public class PreMarketPriceMilestoneCache {

    private final StocksCache stocksCache;
    private final DailyPricesCache dailyPricesCache;

    public List<String> findTickersForPreMarketMilestone(PreMarketPriceMilestone milestone, List<Double> cfdMargins) {
        Map<String, DailyPrice> preMarketPricesCache = dailyPricesCache.dailyPrices(PRE)
                .stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, p -> p));
        Collection<Stock> stocksList = stocksCache.getStocksMap().values();

        return stocksList.stream()
                .filter(stock -> cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin()))
                .filter(stock -> preMarketPricesCache.containsKey(stock.getTicker()))
                .filter(stock -> priceWithinMilestone(stock, preMarketPricesCache.get(stock.getTicker()), milestone))
                .map(Stock::getTicker)
                .toList();
    }

    private boolean priceWithinMilestone(Stock s, DailyPrice dailyPrice, PreMarketPriceMilestone milestone) {
        return switch (milestone) {
            case GAP_UP -> dailyPrice.getClose() > s.getDailyClose();
            case GAP_DOWN -> dailyPrice.getClose() < s.getDailyClose();
            case GAP_UP_10_PERCENT -> dailyPrice.getClose() > s.getDailyClose() * 1.10;
            case GAP_DOWN_10_PERCENT -> dailyPrice.getClose() < s.getDailyClose() * 0.90;
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

}