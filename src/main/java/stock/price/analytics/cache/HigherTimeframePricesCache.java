package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.model.MonthlyPriceWithPrevClose;
import stock.price.analytics.cache.model.QuarterlyPriceWithPrevClose;
import stock.price.analytics.cache.model.WeeklyPriceWithPrevClose;
import stock.price.analytics.cache.model.YearlyPriceWithPrevClose;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Getter
public class HigherTimeframePricesCache {

    private final Map<String, WeeklyPrice> weeklyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, MonthlyPrice> monthlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, QuarterlyPrice> quarterlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, YearlyPrice> yearlyPricesByTickerAndDate = new HashMap<>();

    private final Map<String, WeeklyPriceWithPrevClose> weeklyPricesWithPrevCloseByTickerAndDate = new HashMap<>();
    private final Map<String, MonthlyPriceWithPrevClose> monthlyPricesWithPrevCloseByTickerAndDate = new HashMap<>();
    private final Map<String, QuarterlyPriceWithPrevClose> quarterlyPricesWithPrevCloseByTickerAndDate = new HashMap<>();
    private final Map<String, YearlyPriceWithPrevClose> yearlyPricesWithPrevCloseByTickerAndDate = new HashMap<>();

    public void addWeeklyPriceWithPrevClose(List<WeeklyPriceWithPrevClose> weeklyPricesWithPrevClose) {
        weeklyPricesWithPrevClose.forEach(price -> {
            weeklyPricesWithPrevCloseByTickerAndDate.merge(
                    createKey(price.weeklyPrice().getTicker(), price.weeklyPrice().getStartDate()),
                    price,
                    (_, newPrice) -> newPrice // Logic to keep the latest price
            );
        });
    }

    public void addMonthlyPriceWithPrevClose(List<MonthlyPriceWithPrevClose> monthlyPricesWithPrevClose) {
        monthlyPricesWithPrevClose.forEach(price -> {
            monthlyPricesWithPrevCloseByTickerAndDate.merge(
                    createKey(price.monthlyPrice().getTicker(), price.monthlyPrice().getStartDate()),
                    price,
                    (_, newPrice) -> newPrice // Logic to keep the latest price
            );
        });
    }

    public void addQuarterlyPriceWithPrevClose(List<QuarterlyPriceWithPrevClose> quarterlyPricesWithPrevClose) {
        quarterlyPricesWithPrevClose.forEach(price -> {
            quarterlyPricesWithPrevCloseByTickerAndDate.merge(
                    createKey(price.quarterlyPrice().getTicker(), price.quarterlyPrice().getStartDate()),
                    price,
                    (_, newPrice) -> newPrice // Logic to keep the latest price
            );
        });
    }

    public void addYearlyPriceWithPrevClose(List<YearlyPriceWithPrevClose> yearlyPricesWithPrevClose) {
        yearlyPricesWithPrevClose.forEach(price -> {
            yearlyPricesWithPrevCloseByTickerAndDate.merge(
                    createKey(price.yearlyPrice().getTicker(), price.yearlyPrice().getStartDate()),
                    price,
                    (_, newPrice) -> newPrice // Logic to keep the latest price
            );
        });
    }

    public List<WeeklyPriceWithPrevClose> weeklyPricesWithPrevCloseFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        weeklyPricesWithPrevCloseByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<MonthlyPriceWithPrevClose> monthlyPricesWithPrevCloseFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        monthlyPricesWithPrevCloseByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<QuarterlyPriceWithPrevClose> quarterlyPricesWithPrevCloseFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        quarterlyPricesWithPrevCloseByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<YearlyPriceWithPrevClose> yearlyPricesWithPrevCloseFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        yearlyPricesWithPrevCloseByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public void addPrices(List<? extends AbstractPrice> prices) {
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

    public List<? extends AbstractPrice> pricesFor(List<String> tickers, StockTimeframe timeframe) {
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