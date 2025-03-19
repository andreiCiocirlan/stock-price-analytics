package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.nio.file.Files.readAllLines;
import static java.time.LocalDate.parse;

@Component
@Slf4j
public class PricesUtil {
    public static final AtomicInteger OPEN_IS_ZERO_ERROR = new AtomicInteger(0);
    public static final AtomicInteger HIGH_LOW_ERROR = new AtomicInteger(0);

    public static List<AbstractPrice> pricesForFileAndTimeframe(Path srcFile, StockTimeframe stockTimeframe) {
        List<DailyPrice> dailyPrices = dailyPricesFromFile(srcFile);

        if (StockTimeframe.DAILY == stockTimeframe)
            return new ArrayList<>(dailyPrices);

        return htfPricesForTimeframe(dailyPrices, stockTimeframe);
    }

    public static List<AbstractPrice> htfPricesForTimeframe(List<DailyPrice> dailyPrices, StockTimeframe stockTimeframe) {
        return new ArrayList<>(
                dailyPrices.stream()
                        .collect(Collectors.groupingBy(
                                shp -> groupingFunctionFor(stockTimeframe).apply(shp.getDate()),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        prices -> extractPriceForTimeframe(prices, stockTimeframe)
                                )
                        )).values());
    }

    private static Function<LocalDate, ? extends Temporal> groupingFunctionFor(StockTimeframe stockTimeframe) {
        return switch (stockTimeframe) {
            case WEEKLY -> shp -> shp.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> YearMonth::from;
            case QUARTERLY -> shp -> YearMonth.of(shp.getYear(), shp.getMonth().firstMonthOfQuarter().getValue());
            case YEARLY -> Year::from;
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
        };
    }

    private static AbstractPrice extractPriceForTimeframe(List<DailyPrice> pricesGroupedByTimeFrame, StockTimeframe stockTimeframe) {
        DailyPrice firstInChronologicalOrder = pricesGroupedByTimeFrame.getFirst(); // already sorted
        DailyPrice lastInChronologicalOrder = pricesGroupedByTimeFrame.getLast();
        String ticker = firstInChronologicalOrder.getTicker();
        LocalDate startDate = firstInChronologicalOrder.getDate();
        double open = firstInChronologicalOrder.getOpen();
        double close = lastInChronologicalOrder.getClose();
        double high = pricesGroupedByTimeFrame.stream()
                .mapToDouble(DailyPrice::getHigh)
                .max()
                .orElseThrow();
        double low = pricesGroupedByTimeFrame.stream()
                .mapToDouble(DailyPrice::getLow)
                .min()
                .orElseThrow();

        CandleOHLC candleOHLC = new CandleOHLC(open, high, low, close);
        return switch (stockTimeframe) {
            case WEEKLY -> new WeeklyPrice(ticker, startDate, candleOHLC);
            case MONTHLY -> new MonthlyPrice(ticker, startDate, candleOHLC);
            case QUARTERLY -> new QuarterlyPrice(ticker, startDate, candleOHLC);
            case YEARLY -> new YearlyPrice(ticker, startDate, candleOHLC);
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
        };
    }

    public static List<DailyPrice> dailyPricesFromFile(Path srcFile) {
        List<DailyPrice> dailyPrices = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);
        try {
            readAllLines(srcFile).stream().skip(1).parallel().forEachOrdered(line -> addDailyPrices(line, ticker, dailyPrices));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dailyPrices;
    }

    public static List<DailyPrice> dailyPricesFromFileWithDate(Path srcFile, LocalDate tradingDate) {
        List<DailyPrice> dailyPrices = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);

        List<String> lines;
        try {
            lines = readAllLines(srcFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LocalDate prevDayTradingDate = TradingDateUtil.previousTradingDate(tradingDate);
        lines.stream()
                .filter(line -> line.contains(tradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        || line.contains(prevDayTradingDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .parallel().forEachOrdered(line -> addDailyPrices(line, ticker, dailyPrices));
        return dailyPrices;
    }

    public static List<DailyPrice> dailyPricesFromFileWithCount(Path srcFile, int lastDays) {
        List<DailyPrice> dailyPrices = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);

        List<String> lines;
        try {
            lines = readAllLines(srcFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int skipCount = Math.max(1, lines.size() - lastDays);
        lines.stream().skip(skipCount).parallel().forEachOrdered(line -> addDailyPrices(line, ticker, dailyPrices));
        return dailyPrices;
    }

    private static void addDailyPrices(String line, String ticker, List<DailyPrice> dailyPrices) {
        String[] split = line.split(",");
        if (split.length != 6) {
            throw new RuntimeException("Not all fields found!");
        }
        try {
            dailyPrices.add(new DailyPrice(ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE),
                    new CandleOHLC(parseDouble(split[2]), parseDouble(split[3]), parseDouble(split[4]), parseDouble(split[5]))));
        } catch (NumberFormatException e) {
            log.error("HIGH_LOW_ERROR ticker {} date {} error: {}", ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE), e.getMessage());
            HIGH_LOW_ERROR.incrementAndGet();
        } catch (IllegalArgumentException e) {
            log.error("OPEN_IS_ZERO_ERROR ticker {} date {} error: {}", ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE), e.getMessage());
            OPEN_IS_ZERO_ERROR.incrementAndGet();
        }
    }

    public static String tickerFrom(Path srcFile) {
        String fileName = srcFile.getFileName().toString();
        return fileName.substring(0, fileName.length() - 4); // remove .csv
    }


}