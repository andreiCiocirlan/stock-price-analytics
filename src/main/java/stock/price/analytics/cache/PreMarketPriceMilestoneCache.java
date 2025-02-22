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
class PreMarketPriceMilestoneCache {

    private static final Double MIN_GAP_AND_GO_PERCENTAGE = 0.04d;
    private final StocksCache stocksCache;
    private final DailyPricesCache dailyPricesCache;

    List<String> findTickersForPreMarketMilestone(PreMarketPriceMilestone milestone, List<Double> cfdMargins) {
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

    private boolean priceWithinMilestone(Stock s, DailyPrice preMarketPrice, PreMarketPriceMilestone milestone) {
        return switch (milestone) {
            // previous day DOWN more than 1% && pre-market GAP UP more than 4%
            case KICKING_CANDLE_UP -> s.getDailyPerformance() < -1.0d && preMarketPrice.getClose() > s.getDailyClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // previous day UP more than 1% && pre-market GAP DOWN more than 4%
            case KICKING_CANDLE_DOWN -> s.getDailyPerformance() > 1.0d && preMarketPrice.getClose() < s.getDailyClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            case GAP_UP -> preMarketPrice.getClose() > s.getDailyClose();
            case GAP_DOWN -> preMarketPrice.getClose() < s.getDailyClose();
            case GAP_UP_10_PERCENT -> preMarketPrice.getClose() > s.getDailyClose() * 1.10;
            case GAP_DOWN_10_PERCENT -> preMarketPrice.getClose() < s.getDailyClose() * 0.90;
            // pre-market GAP UP more than 4%
            case GAP_UP_AND_GO -> preMarketPrice.getClose() > s.getDailyClose() * (1 + MIN_GAP_AND_GO_PERCENTAGE);
            // pre-market GAP DOWN more than 4%
            case GAP_DOWN_AND_GO -> preMarketPrice.getClose() < s.getDailyClose() * (1 - MIN_GAP_AND_GO_PERCENTAGE);
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

}