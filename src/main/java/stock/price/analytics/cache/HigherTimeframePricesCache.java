package stock.price.analytics.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.MonthlyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.YearlyPriceOHLC;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HigherTimeframePricesCache {

    private final Map<String, WeeklyPriceOHLC> weeklyPricesByTickerAndDate = new TreeMap<>(Collections.reverseOrder());
    private final Map<String, MonthlyPriceOHLC> monthlyPricesByTickerAndDate = new TreeMap<>(Collections.reverseOrder());
    private final Map<String, YearlyPriceOHLC> yearlyPricesByTickerAndDate = new TreeMap<>(Collections.reverseOrder());

    public void addWeeklyPrices(List<WeeklyPriceOHLC> weeklyPrices) {
        weeklyPrices.forEach(price ->
                this.weeklyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addMonthlyPrices(List<MonthlyPriceOHLC> monthlyPrices) {
        monthlyPrices.forEach(price ->
                this.monthlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addYearlyPrices(List<YearlyPriceOHLC> yearlyPrices) {
        yearlyPrices.forEach(price ->
                this.yearlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public List<WeeklyPriceOHLC> weeklyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        weeklyPricesByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<MonthlyPriceOHLC> monthlyPricesFor(List<String> tickers) {
        return tickers.stream()
                .flatMap(ticker ->
                        monthlyPricesByTickerAndDate.entrySet().stream()
                                .filter(entry -> entry.getKey().startsWith(ticker + "_"))
                                .map(Map.Entry::getValue))
                .collect(Collectors.toList());
    }

    public List<YearlyPriceOHLC> yearlyPricesFor(List<String> tickers) {
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

    public Set<String> yearlyPricesTickers() {
        return yearlyPricesByTickerAndDate.keySet().stream().map(key -> key.split("_")[0]).collect(Collectors.toSet());
    }

    private String createKey(String ticker, LocalDate startDate) {
        return ticker + "_" + startDate;
    }
}