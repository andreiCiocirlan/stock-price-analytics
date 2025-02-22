package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HigherTimeframePricesCacheService {

    private final HigherTimeframePricesCache higherTimeframePricesCache;

    public Map<String, ? extends AbstractPrice> getPricesByTickerAndDateFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> higherTimeframePricesCache.getWeeklyPricesByTickerAndDate();
            case MONTHLY -> higherTimeframePricesCache.getMonthlyPricesByTickerAndDate();
            case QUARTERLY -> higherTimeframePricesCache.getQuarterlyPricesByTickerAndDate();
            case YEARLY -> higherTimeframePricesCache.getYearlyPricesByTickerAndDate();
        };
    }

    public void addPrices(List<? extends AbstractPrice> prices) {
        higherTimeframePricesCache.addPrices(prices);
    }

    public List<? extends AbstractPrice> pricesFor(List<String> tickers, StockTimeframe timeframe) {
        return higherTimeframePricesCache.pricesFor(tickers, timeframe);
    }
}
