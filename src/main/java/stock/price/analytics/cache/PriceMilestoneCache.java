package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.PriceMilestone;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.stocks.Stock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceMilestoneCache {

    private final StocksCache stocksCache;
    private final HighLowPricesCache highLowPricesCache;

    public List<String> findTickersForMilestone(PriceMilestone priceMilestone, double cfdMargin) {
        Map<String, HighLowForPeriod> hlPricesCache = highLowPricesCache.cacheForMilestone(priceMilestone)
                .stream()
                .collect(Collectors.toMap(HighLowForPeriod::getTicker, p -> p));
        Collection<Stock> stocksList = stocksCache.getStocksMap().values();
        List<Stock> stocks = stocksList.stream()
                .filter(stock -> stock.getCfdMargin() == cfdMargin)
                .filter(stock -> priceWithinMilestone(stock, hlPricesCache.get(stock.getTicker()), priceMilestone))
                .toList();

        return stocks.stream().map(Stock::getTicker).toList();
    }

    private boolean priceWithinMilestone(Stock s, HighLowForPeriod highLowForPeriod, PriceMilestone priceMilestone) {
        return switch (priceMilestone) {
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