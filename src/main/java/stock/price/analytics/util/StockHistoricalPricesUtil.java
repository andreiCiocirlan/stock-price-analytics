package stock.price.analytics.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.ohlc.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.nio.file.Files.readAllLines;
import static java.time.LocalDate.parse;
import static java.util.Comparator.comparing;

@Component
@Slf4j
public class StockHistoricalPricesUtil {

    public static List<WeeklyPriceOHLC> weeklyPricesFromFile(Path srcFile) throws IOException {
        return weeklyPricesFrom(dailyPricesFromFile(srcFile));
    }

    public static List<WeeklyPriceOHLC> weeklyOHLCPerfFromFile(Path srcFile) throws IOException {
        return weeklyExtractFrom(dailyPricesFromFile(srcFile));
    }

    public static List<MonthlyPriceOHLC> monthlyPricesFromFile(Path srcFile) throws IOException {
        return monthlyExtractFrom(dailyPricesFromFile(srcFile));
    }

    public static List<YearlyPriceOHLC> yearlyPricesFromFile(Path srcFile) throws IOException {
        return yearlyExtractFrom(dailyPricesFromFile(srcFile));
    }

    private static List<YearlyPriceOHLC> yearlyExtractFrom(List<DailyPriceOHLC> dailyPrices) {
        return extractByGroupingForTimeframe(dailyPrices, Year::from, StockHistoricalPricesUtil::extractYearlyPricesFrom);
    }

    private static List<MonthlyPriceOHLC> monthlyExtractFrom(List<DailyPriceOHLC> dailyPrices) {
        return extractByGroupingForTimeframe(dailyPrices, YearMonth::from, StockHistoricalPricesUtil::extractMonthlyPricesFrom);
    }

//    private static List<YearlyOHLCPerf> weeklyExtractFrom(List<StockHistoricalPrices> dailyPrices) {
//        return extractByGroupingForTimeframe(dailyPrices, Year::from, StockHistoricalPricesUtil::extractYearlyPricesFrom);
//    }

    private static List<WeeklyPriceOHLC> weeklyExtractFrom(List<DailyPriceOHLC> dailyPrices) {
        return extractByGroupingForTimeframe(dailyPrices, shp -> shp.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), StockHistoricalPricesUtil::eextractWeeklyPricesFrom);
    }

    private static WeeklyPriceOHLC eextractWeeklyPricesFrom(List<DailyPriceOHLC> groupedByWeekStockPrices) {
        WeeklyPriceOHLC weeklyOHLCPerf = new WeeklyPriceOHLC();
        DailyPriceOHLC firstInChronologicalOrder = groupedByWeekStockPrices.get(0); // already sorted -> get(0) is first in chronological order
        DailyPriceOHLC lastInChronologicalOrder = groupedByWeekStockPrices.get(groupedByWeekStockPrices.size() - 1);
        weeklyOHLCPerf.setTicker(firstInChronologicalOrder.getTicker());
        weeklyOHLCPerf.setStartDate(firstInChronologicalOrder.getDate());
        weeklyOHLCPerf.setEndDate(lastInChronologicalOrder.getDate());
        weeklyOHLCPerf.setOpen(firstInChronologicalOrder.getOpen());
        weeklyOHLCPerf.setClose(lastInChronologicalOrder.getClose());
        weeklyOHLCPerf.setHigh(groupedByWeekStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getHigh)
                .max()
                .orElseThrow());
        weeklyOHLCPerf.setLow(groupedByWeekStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getLow)
                .min()
                .orElseThrow());
        return weeklyOHLCPerf;
    }

    private static MonthlyPriceOHLC extractMonthlyPricesFrom(List<DailyPriceOHLC> groupedByMonthStockPrices) {
        DailyPriceOHLC firstInChronologicalOrder = groupedByMonthStockPrices.get(0); // already sorted -> get(0) is first in chronological order
        DailyPriceOHLC lastInChronologicalOrder = groupedByMonthStockPrices.get(groupedByMonthStockPrices.size() - 1);
        String ticker = firstInChronologicalOrder.getTicker();
        LocalDate startDate = groupedByMonthStockPrices.stream()
                .map(DailyPriceOHLC::getDate)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate endDate = groupedByMonthStockPrices.stream()
                .map(DailyPriceOHLC::getDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        double open = firstInChronologicalOrder.getOpen();
        double close = lastInChronologicalOrder.getClose();
        double high = groupedByMonthStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getHigh)
                .max()
                .orElseThrow();
        double low = groupedByMonthStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getLow)
                .min()
                .orElseThrow();

        return new MonthlyPriceOHLC(ticker, startDate, endDate, new CandleOHLC(open, high, low, close));
    }

    private static YearlyPriceOHLC extractYearlyPricesFrom(List<DailyPriceOHLC> groupedByYearStockPrices) {
        DailyPriceOHLC firstInChronologicalOrder = groupedByYearStockPrices.get(0); // already sorted -> get(0) is first in chronological order
        DailyPriceOHLC lastInChronologicalOrder = groupedByYearStockPrices.get(groupedByYearStockPrices.size() - 1);
        String ticker = firstInChronologicalOrder.getTicker();
        LocalDate startDate = groupedByYearStockPrices.stream()
                .map(DailyPriceOHLC::getDate)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        LocalDate endDate = groupedByYearStockPrices.stream()
                .map(DailyPriceOHLC::getDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        double open = firstInChronologicalOrder.getOpen();
        double close = lastInChronologicalOrder.getClose();
        double high = groupedByYearStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getHigh)
                .max()
                .orElseThrow();
        double low = groupedByYearStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getLow)
                .min()
                .orElseThrow();

        return new YearlyPriceOHLC(ticker, startDate, endDate, new CandleOHLC(open, high, low, close));
    }


    private static <T extends AbstractPriceOHLC> List<T> extractByGroupingForTimeframe(List<DailyPriceOHLC> dailyPrices, Function<LocalDate, ? extends Temporal> groupingFunction, Function<List<DailyPriceOHLC>, T> extractionFunction) {
        return new ArrayList<>(dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        shp -> groupingFunction.apply(shp.getDate()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                extractionFunction
                        )
                )).values());
    }

    private static List<YearlyPriceOHLC> yearlyPricesFrom(List<DailyPriceOHLC> dailyPrices) {
        return new ArrayList<>(dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        shp -> Year.from(shp.getDate()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                StockHistoricalPricesUtil::extractYearlyPricesFrom
                        )
                ))
                .values());
    }

    private static List<MonthlyPriceOHLC> monthlyPricesFrom(List<DailyPriceOHLC> dailyPrices) {
        return new ArrayList<>(dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        shp -> YearMonth.from(shp.getDate()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                StockHistoricalPricesUtil::extractMonthlyPricesFrom
                        )
                ))
                .values());
    }

    protected static List<WeeklyPriceOHLC> weeklyPricesFrom(List<DailyPriceOHLC> dailyPrices) {
        return new ArrayList<>(dailyPrices.stream()
                .collect(Collectors.groupingBy(
                        shp -> shp.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                StockHistoricalPricesUtil::extractWeeklyPricesFrom
                        )
                ))
                .values())
                .stream()
                .sorted(comparing(WeeklyPriceOHLC::getStartDate)) // chronological order
                .toList();
    }

    private static WeeklyPriceOHLC extractWeeklyPricesFrom(List<DailyPriceOHLC> groupedByWeekStockPrices) {
        WeeklyPriceOHLC weeklyPrices = new WeeklyPriceOHLC();
        DailyPriceOHLC mondayPrices = groupedByWeekStockPrices.getFirst();
        DailyPriceOHLC fridayPrices = groupedByWeekStockPrices.getLast();
        weeklyPrices.setTicker(mondayPrices.getTicker());
        weeklyPrices.setStartDate(mondayPrices.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        weeklyPrices.setEndDate(fridayPrices.getDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)));
        weeklyPrices.setOpen(mondayPrices.getOpen());
        weeklyPrices.setClose(fridayPrices.getClose());
        weeklyPrices.setHigh(groupedByWeekStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getHigh)
                .max()
                .orElseThrow());
        weeklyPrices.setLow(groupedByWeekStockPrices.stream()
                .mapToDouble(DailyPriceOHLC::getLow)
                .min()
                .orElseThrow());
        return weeklyPrices;
    }

    public static Pair<List<DailyPriceOHLC>, List<WeeklyPriceOHLC>> weeklyPricesFromFileLastDays(Path srcFile, int lastDays) throws IOException {
        List<DailyPriceOHLC> dailyPrices = dailyPricesFromFileLastDays(srcFile, lastDays);
        List<WeeklyPriceOHLC> weeklyPrices = weeklyPricesFrom(dailyPrices);
        return Pair.of(dailyPrices, weeklyPrices);
    }

    public static List<DailyPriceOHLC> dailyPricesFromFile(Path srcFile) throws IOException {
        List<DailyPriceOHLC> dailyPriceOHLCS = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);
        readAllLines(srcFile).stream().skip(1).parallel().forEachOrdered(line -> addStockHistoricalPricesToList(line, ticker, dailyPriceOHLCS));
        return dailyPriceOHLCS;
    }

    public static List<DailyPriceOHLC> dailyPricesFromFileLastDays(Path srcFile, int lastDays) throws IOException {
        List<DailyPriceOHLC> dailyPrices = new ArrayList<>();
        final String ticker = tickerFrom(srcFile);

        List<String> lines = Files.readAllLines(srcFile);
        int skipCount = Math.max(1, lines.size() + 1 - lastDays);
        lines.stream().skip(skipCount)
                .parallel().forEachOrdered(line -> addStockHistoricalPricesToList(line, ticker, dailyPrices));
        return dailyPrices;
    }

    private static void addStockHistoricalPricesToList(String line, String ticker, List<DailyPriceOHLC> stockHistoricalPricesList) {
        String[] split = line.split(",");
        if (split.length != 6) {
            throw new RuntimeException("Mismatch number of fields found! Expected 6, found " + split.length);
        }
        stockHistoricalPricesList.add(new DailyPriceOHLC(ticker, parse(split[1], DateTimeFormatter.ISO_LOCAL_DATE),
                new CandleOHLC(parseDouble(split[2]), parseDouble(split[3]), parseDouble(split[4]), parseDouble(split[5]))));
    }

    public static <T, R extends JpaRepository<T, ?>> void partitionDataAndPersistt(List<T> entities, R repository) {
        List<List<T>> partitions = new ArrayList<>();
        int batchSize = 250;
        for (int i = 0; i < entities.size(); i += batchSize) {
            partitions.add(entities.subList(i, Math.min(i + batchSize,
                    entities.size())));
        }
        saveHistoricalPrices(partitions, repository, Constants.NR_THREADS);
        log.info("Saved {} rows of type: {} ", entities.size(), entities.get(0).getClass().getName());
    }

    private static <T, R extends JpaRepository<T, ?>> void saveHistoricalPrices(List<List<T>> partitions, R repository, int nrThreads) {
        List<Callable<Void>> callables = partitions.stream().map(sublist ->
                (Callable<Void>) () -> {
                    repository.saveAll(sublist);
                    return null;
                }).collect(Collectors.toList());
        ExecutorService executorService = Executors.newFixedThreadPool(nrThreads);
        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

    private static String tickerFrom(Path srcFile) {
        String fileName = srcFile.getFileName().toString();
        return fileName.substring(0, fileName.length() - 4); // remove .csv
    }
}