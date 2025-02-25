package stock.price.analytics.cache;

import lombok.Getter;
import org.aspectj.apache.bcel.generic.RET;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.model.PriceWithPrevClose;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static stock.price.analytics.model.prices.enums.StockTimeframe.*;

@Component
@Getter
class HigherTimeframePricesCache {

    private final Map<String, AbstractPrice> weeklyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, AbstractPrice> monthlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, AbstractPrice> quarterlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, AbstractPrice> yearlyPricesByTickerAndDate = new HashMap<>();

    private final Map<StockTimeframe, Map<String, AbstractPrice>> pricesByTimeframe = Map.of(
            WEEKLY, weeklyPricesByTickerAndDate,
            MONTHLY, monthlyPricesByTickerAndDate,
            QUARTERLY, quarterlyPricesByTickerAndDate,
            YEARLY, yearlyPricesByTickerAndDate
    );

    private final Map<String, PriceWithPrevClose> weeklyPricesWithPrevCloseByTicker = new HashMap<>();
    private final Map<String, PriceWithPrevClose> monthlyPricesWithPrevCloseByTicker = new HashMap<>();
    private final Map<String, PriceWithPrevClose> quarterlyPricesWithPrevCloseByTicker = new HashMap<>();
    private final Map<String, PriceWithPrevClose> yearlyPricesWithPrevCloseByTicker = new HashMap<>();

    private final Map<StockTimeframe, Map<String, PriceWithPrevClose>> pricesWithPrevCloseByTimeframe = Map.of(
            WEEKLY, weeklyPricesWithPrevCloseByTicker,
            MONTHLY, monthlyPricesWithPrevCloseByTicker,
            QUARTERLY, quarterlyPricesWithPrevCloseByTicker,
            YEARLY, yearlyPricesWithPrevCloseByTicker
    );

    void addPricesWithPrevClose(List<PriceWithPrevClose> pricesWithPrevClose, StockTimeframe timeframe) {
        if (pricesWithPrevClose == null || pricesWithPrevClose.isEmpty())
            return; // Handle null or empty list case to avoid exceptions
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevCloseByTimeframe.get(timeframe);
        pricesWithPrevClose.forEach(price -> pricesWithPrevCloseByTicker.put(price.getPrice().getTicker(), price));
    }

    List<PriceWithPrevClose> pricesWithPrevCloseFor(List<String> tickers, StockTimeframe timeframe) {
        Map<String, PriceWithPrevClose> pricesWithPrevCloseByTicker = pricesWithPrevCloseByTimeframe.get(timeframe);
        return tickers.stream()
                .flatMap(ticker -> pricesWithPrevCloseByTicker.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(ticker))
                        .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    void addPrices(List<? extends AbstractPrice> prices) {
        if (prices == null || prices.isEmpty()) return; // Handle null or empty list case to avoid exceptions

        prices.forEach(price -> {
            Map<String, AbstractPrice> pricesByTickerAndStartDate = pricesByTimeframe.get(prices.getFirst().getTimeframe());
            pricesByTickerAndStartDate.put(createKey(price.getTicker(), price.getStartDate()), price);
        });
    }

    List<? extends AbstractPrice> pricesFor(List<String> tickers, StockTimeframe timeframe) {
        return tickers.stream()
                .flatMap(ticker -> switch (timeframe) {
                    case WEEKLY -> weeklyPricesByTickerAndDate.entrySet().stream()
                            .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                            .map(Map.Entry::getValue);
                    case MONTHLY -> monthlyPricesByTickerAndDate.entrySet().stream()
                            .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                            .map(Map.Entry::getValue);
                    case QUARTERLY -> quarterlyPricesByTickerAndDate.entrySet().stream()
                            .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                            .map(Map.Entry::getValue);
                    case YEARLY -> yearlyPricesByTickerAndDate.entrySet().stream()
                            .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                            .map(Map.Entry::getValue);
                    default -> throw new IllegalArgumentException("Unexpected timeframe: " + timeframe);
                })
                .collect(Collectors.toList());
    }

    private String createKey(String ticker, LocalDate startDate) {
        return ticker + "_" + startDate;
    }
}