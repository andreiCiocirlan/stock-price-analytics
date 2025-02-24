package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.model.*;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.util.*;
import java.util.stream.Collectors;

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

    public <T extends AbstractPrice> List<PriceWithPrevClose> pricesWithPrevCloseByTickerFrom(List<T> previousThreePricesForTickers) {
        Map<String, List<T>> previousTwoPricesByTicker = previousThreePricesForTickers
                .stream()
                .collect(Collectors.groupingBy(AbstractPrice::getTicker))
                .values().stream()
                .flatMap(prices -> prices.stream().sorted(Comparator.comparing(AbstractPrice::getStartDate).reversed()).limit(2))
                .collect(Collectors.groupingBy(AbstractPrice::getTicker));
        List<T> latestPrices = new ArrayList<>();
        Map<String, Double> previousCloseByTicker = new HashMap<>();
        for (List<T> prices : previousTwoPricesByTicker.values()) {
            latestPrices.add(prices.get(0)); // most recent price
            previousCloseByTicker.put(prices.get(0).getTicker(),
                    prices.size() > 1 ? prices.get(1).getClose() : prices.get(0).getOpen()); // if IPO week, month, quarter, year -> take opening price
        }

        return latestPrices.stream()
                .map(price -> (PriceWithPrevClose) switch (price.getTimeframe()) {
                    case DAILY -> throw new IllegalStateException("Unexpected timeframe DAILY");
                    case WEEKLY -> new WeeklyPriceWithPrevClose((WeeklyPrice) price, previousCloseByTicker.get(price.getTicker()));
                    case MONTHLY -> new MonthlyPriceWithPrevClose((MonthlyPrice) price, previousCloseByTicker.get(price.getTicker()));
                    case QUARTERLY -> new QuarterlyPriceWithPrevClose((QuarterlyPrice) price, previousCloseByTicker.get(price.getTicker()));
                    case YEARLY -> new YearlyPriceWithPrevClose((YearlyPrice) price, previousCloseByTicker.get(price.getTicker()));
                })
                .toList();
    }

    public void addPrices(List<? extends AbstractPrice> prices) {
        higherTimeframePricesCache.addPrices(prices);
    }

    public List<? extends AbstractPrice> htfPricesFor(List<String> tickers, StockTimeframe timeframe) {
        return higherTimeframePricesCache.pricesFor(tickers, timeframe);
    }

    public void addPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose) {
        higherTimeframePricesCache.addPricesWithPrevClose(pricesWithPrevClose, pricesWithPrevClose.getFirst().getPrice().getTimeframe());
    }

    public List<PriceWithPrevClose> pricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe) {
        return higherTimeframePricesCache.pricesWithPrevCloseFor(tickers, timeframe);
    }

}
