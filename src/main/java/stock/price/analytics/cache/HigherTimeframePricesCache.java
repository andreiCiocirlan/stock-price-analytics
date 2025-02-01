package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.MonthlyPrice;
import stock.price.analytics.model.prices.ohlc.QuarterlyPrice;
import stock.price.analytics.model.prices.ohlc.WeeklyPrice;
import stock.price.analytics.model.prices.ohlc.YearlyPrice;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HigherTimeframePricesCache {

    private final Map<String, WeeklyPrice> weeklyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, MonthlyPrice> monthlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, QuarterlyPrice> quarterlyPricesByTickerAndDate = new HashMap<>();
    private final Map<String, YearlyPrice> yearlyPricesByTickerAndDate = new HashMap<>();

    public void addWeeklyPrices(List<WeeklyPrice> weeklyPrices) {
        weeklyPrices.forEach(price ->
                this.weeklyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addMonthlyPrices(List<MonthlyPrice> monthlyPrices) {
        monthlyPrices.forEach(price ->
                this.monthlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addQuarterlyPrices(List<QuarterlyPrice> quarterlyPrices) {
        quarterlyPrices.forEach(price ->
                this.quarterlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addYearlyPrices(List<YearlyPrice> yearlyPrices) {
        yearlyPrices.forEach(price ->
                this.yearlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public List<WeeklyPrice> weeklyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        weeklyPricesByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<MonthlyPrice> monthlyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        monthlyPricesByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<QuarterlyPrice> quarterlyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        quarterlyPricesByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<YearlyPrice> yearlyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        yearlyPricesByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public Set<String> weeklyPricesTickers() {
        return weeklyPricesByTickerAndDate.keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toSet());
    }

    public Set<String> monthlyPricesTickers() {
        return monthlyPricesByTickerAndDate.keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toSet());
    }

    public Set<String> quarterlyPricesTickers() {
        return quarterlyPricesByTickerAndDate.keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toSet());
    }

    public Set<String> yearlyPricesTickers() {
        return yearlyPricesByTickerAndDate.keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toSet());
    }

    private String createKey(String ticker, LocalDate startDate) {
        return ticker + "_" + startDate;
    }
}