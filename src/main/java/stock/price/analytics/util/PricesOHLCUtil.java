package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class PricesOHLCUtil {
    public static final AtomicInteger OPEN_IS_ZERO_ERROR = new AtomicInteger(0);
    public static final AtomicInteger HIGH_LOW_ERROR = new AtomicInteger(0);

    public static List<AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) throws IOException {
        List<DailyPriceOHLC> dailyPrices = dailyPricesOHLCFromFile(Paths.get(Constants.STOCKS_LOCATION));

        return getPriceOHLCsForTimeframe(dailyPrices, stockTimeframe);
    }

    public static List<AbstractPriceOHLC> getPriceOHLCsForTimeframe(List<DailyPriceOHLC> dailyPrices, StockTimeframe stockTimeframe) {
        return new ArrayList<>(
                dailyPrices.stream()
                        .collect(Collectors.groupingBy(
                                shp -> groupingFunctionFor(stockTimeframe).apply(shp.getDate()),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        pricesOHLC -> extractOHLCForTimeframe(pricesOHLC, stockTimeframe)
                                )
                        )).values());
    }

    private static Function<LocalDate, ? extends Temporal> groupingFunctionFor(StockTimeframe stockTimeframe) {
        return switch (stockTimeframe) {
            case WEEKLY -> shp -> shp.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> YearMonth::from;
            case YEARLY -> Year::from;
        };
    }

    private static AbstractPriceOHLC extractOHLCForTimeframe(List<DailyPriceOHLC> pricesGroupedByTimeFrame, StockTimeframe stockTimeframe) {
        DailyPriceOHLC firstInChronologicalOrder = pricesGroupedByTimeFrame.getFirst(); // already sorted
        DailyPriceOHLC lastInChronologicalOrder = pricesGroupedByTimeFrame.getLast();
        String ticker = firstInChronologicalOrder.getTicker();
        LocalDate startDate = firstInChronologicalOrder.getDate();
        LocalDate endDate = lastInChronologicalOrder.getDate();
        double open = firstInChronologicalOrder.getOpen();
        double close = lastInChronologicalOrder.getClose();
        double high = pricesGroupedByTimeFrame.stream()
                .mapToDouble(DailyPriceOHLC::getHigh)
                .max()
                .orElseThrow();
        double low = pricesGroupedByTimeFrame.stream()
                .mapToDouble(DailyPriceOHLC::getLow)
                .min()
                .orElseThrow();

        CandleOHLC candleOHLC = new CandleOHLC(open, high, low, close);
        return switch (stockTimeframe) {
            case WEEKLY -> new WeeklyPriceOHLC(ticker, startDate, endDate, candleOHLC);
            case MONTHLY -> new MonthlyPriceOHLC(ticker, startDate, endDate, candleOHLC);
            case YEARLY -> new YearlyPriceOHLC(ticker, startDate, endDate, candleOHLC);
        };
    }

    public static List<DailyPriceOHLC> dailyPricesOHLCFromFile(Path srcFile) throws IOException {
        List<DailyPriceOHLC> dailyPricesOHLC = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);
        readAllLines(srcFile).stream().skip(1).parallel().forEachOrdered(line -> addDailyPrices(line, ticker, dailyPricesOHLC));
        return dailyPricesOHLC;
    }

    public static List<DailyPriceOHLC> dailyPricesFromFileLastWeek(Path srcFile, int lastDays) throws IOException {
        List<DailyPriceOHLC> dailyPrices = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);

        List<String> lines = Files.readAllLines(srcFile);
        int skipCount = Math.max(1, lines.size() + 1 - lastDays);
        lines.stream().skip(skipCount).parallel().forEachOrdered(line -> addDailyPrices(line, ticker, dailyPrices));
        return dailyPrices;
    }

    private static void addDailyPrices(String line, String ticker, List<DailyPriceOHLC> dailyPrices) {
        String[] split = line.split(",");
        if (split.length != 6) {
            throw new RuntimeException("Not all fields found!");
        }
        try {
            dailyPrices.add(new DailyPriceOHLC(ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE),
                    new CandleOHLC(parseDouble(split[2]), parseDouble(split[3]), parseDouble(split[4]), parseDouble(split[5]))));
        } catch (NumberFormatException e) {
            log.error("HIGH_LOW_ERROR ticker {} date {} error: {}",ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE), e.getMessage());
            HIGH_LOW_ERROR.incrementAndGet();
        } catch (IllegalArgumentException e) {
            log.error("OPEN_IS_ZERO_ERROR ticker {} date {} error: {}",ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE), e.getMessage());
            OPEN_IS_ZERO_ERROR.incrementAndGet();
        }
    }

    private static String tickerFrom(Path srcFile) {
        String fileName = srcFile.getFileName().toString();
        return fileName.substring(0, fileName.length() - 4); // remove .csv
    }


}