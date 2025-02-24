package stock.price.analytics.cache;

import lombok.Getter;
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

    private final Map<String, WeeklyPrice> weeklyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, MonthlyPrice> monthlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, QuarterlyPrice> quarterlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, YearlyPrice> yearlyPricesByTickerAndDate = new HashMap<>();

    private final Map<String, PriceWithPrevClose> weeklyPricesWithPrevCloseByTickerAndDate = new HashMap<>();
    private final Map<String, PriceWithPrevClose> monthlyPricesWithPrevCloseByTickerAndDate = new HashMap<>();
    private final Map<String, PriceWithPrevClose> quarterlyPricesWithPrevCloseByTickerAndDate = new HashMap<>();
    private final Map<String, PriceWithPrevClose> yearlyPricesWithPrevCloseByTickerAndDate = new HashMap<>();

    private final Map<StockTimeframe, Map<String, PriceWithPrevClose>> pricesWithPrevCloseByTimeframe = Map.of(
            WEEKLY, weeklyPricesWithPrevCloseByTickerAndDate,
            MONTHLY, monthlyPricesWithPrevCloseByTickerAndDate,
            QUARTERLY, quarterlyPricesWithPrevCloseByTickerAndDate,
            YEARLY, yearlyPricesWithPrevCloseByTickerAndDate
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
            switch (prices.getFirst().getTimeframe()) {
                case WEEKLY ->
                        weeklyPricesByTickerAndDate.put(createKey(price.getTicker(), price.getStartDate()), (WeeklyPrice) price);
                case MONTHLY ->
                        monthlyPricesByTickerAndDate.put(createKey(price.getTicker(), price.getStartDate()), (MonthlyPrice) price);
                case QUARTERLY ->
                        quarterlyPricesByTickerAndDate.put(createKey(price.getTicker(), price.getStartDate()), (QuarterlyPrice) price);
                case YEARLY ->
                        yearlyPricesByTickerAndDate.put(createKey(price.getTicker(), price.getStartDate()), (YearlyPrice) price);
                case DAILY -> throw new IllegalStateException("Unexpected timeframe: DAILY");
            }
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