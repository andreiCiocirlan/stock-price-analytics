package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.stocks.Stock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PriceMilestoneCache {

    private final StocksCache stocksCache;
    private final HighLowPricesCache highLowPricesCache;

    public List<String> findTickersForMilestone(PricePerformanceMilestone pricePerformanceMilestone, double cfdMargin) {
        Map<String, HighLowForPeriod> hlPricesCache = highLowPricesCache.cacheForMilestone(pricePerformanceMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, p -> p));
        Collection<Stock> stocksList = stocksCache.getStocksMap().values();

        return stocksList.stream()
                .filter(stock -> stock.getCfdMargin() == cfdMargin)
                .filter(stock -> priceWithinPerformanceMilestone(stock, hlPricesCache.get(stock.getTicker()), pricePerformanceMilestone))
                .map(Stock::getTicker)
                .toList();
    }

    private boolean priceWithinPerformanceMilestone(Stock s, HighLowForPeriod highLowForPeriod, PricePerformanceMilestone pricePerformanceMilestone) {
        return switch (pricePerformanceMilestone) {
            case NEW_52W_HIGH, NEW_ALL_TIME_HIGH, NEW_4W_HIGH -> s.getWeeklyHigh() > highLowForPeriod.getHigh();
            case NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW -> s.getWeeklyLow() < highLowForPeriod.getLow();
            case HIGH_52W_95, HIGH_4W_95, HIGH_ALL_TIME_95 ->
                    highLowForPeriod.getLow() != highLowForPeriod.getHigh() && (1 - (1 - (s.getWeeklyClose() - highLowForPeriod.getLow()) / (highLowForPeriod.getHigh() - highLowForPeriod.getLow()))) > 0.95;
            case LOW_52W_95, LOW_4W_95, LOW_ALL_TIME_95 ->
                    highLowForPeriod.getLow() != highLowForPeriod.getHigh() && (1 - (s.getWeeklyClose() - highLowForPeriod.getLow()) / (highLowForPeriod.getHigh() - highLowForPeriod.getLow())) > 0.95;
            case NONE -> throw new IllegalStateException("Unexpected value NONE");
        };
    }

}