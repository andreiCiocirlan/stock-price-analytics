package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.model.MonthlyPriceWithPrevClose;
import stock.price.analytics.cache.model.QuarterlyPriceWithPrevClose;
import stock.price.analytics.cache.model.WeeklyPriceWithPrevClose;
import stock.price.analytics.cache.model.YearlyPriceWithPrevClose;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.LocalDate;
import java.util.*;
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


    public void addWeeklyPrices(List<WeeklyPrice> weeklyPrices) {
        weeklyPrices.forEach(price ->
                weeklyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addMonthlyPrices(List<MonthlyPrice> monthlyPrices) {
        monthlyPrices.forEach(price ->
                monthlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addQuarterlyPrices(List<QuarterlyPrice> quarterlyPrices) {
        quarterlyPrices.forEach(price ->
                quarterlyPricesByTickerAndDate.merge(
                        createKey(price.getTicker(), price.getStartDate()),
                        price,
                        (_, newPrice) -> newPrice // Logic to keep the latest price
                )
        );
    }

    public void addYearlyPrices(List<YearlyPrice> yearlyPrices) {
        yearlyPrices.forEach(price ->
                yearlyPricesByTickerAndDate.merge(
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