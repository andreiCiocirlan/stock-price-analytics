package stock.price.analytics.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.materializedviews.RefreshMaterializedViewsRepository;
import stock.price.analytics.repository.prices.DailyPricesOHLCRepository;
import stock.price.analytics.repository.prices.PricesOHLCRepository;
import stock.price.analytics.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;
import static stock.price.analytics.util.PricesOHLCUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoricalPricesService {

    private final PricesOHLCRepository pricesOhlcRepository;
    private final DailyPricesOHLCRepository dailyPricesOHLCRepository;
    private final RefreshMaterializedViewsRepository refreshMaterializedViewsRepository;

    private static List<? extends AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) {
        List<AbstractPriceOHLC> priceOHLCS = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        priceOHLCS.addAll(pricesOHLCForFileAndTimeframe(srcFile, stockTimeframe));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return switch (stockTimeframe) {
            case WEEKLY -> priceOHLCS.stream().map(WeeklyPriceOHLC.class::cast).toList();
            case MONTHLY -> priceOHLCS.stream().map(MonthlyPriceOHLC.class::cast).toList();
            case YEARLY -> priceOHLCS.stream().map(YearlyPriceOHLC.class::cast).toList();
        };
    }

    @Transactional
    public void saveLastWeekPricesFromFiles(String tickers) {
        List<DailyPriceOHLC> lastWeekDailyPrices = new ArrayList<>();
        List<AbstractPriceOHLC> lastWeekPrices = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        if (tickers != null && Arrays.stream(tickers.split(",")).noneMatch(ticker -> ticker.equals(tickerFrom(srcFile)))) return;

                        List<DailyPriceOHLC> dailyPriceOHLCS = dailyPricesFromFileLastWeek(srcFile, 500);
                        lastWeekDailyPrices.addAll(dailyPriceOHLCS); // some files might end with empty line -> 7 for good measure instead of 5
//                                lastWeekPrices.addAll(getPriceOHLCsForTimeframe(dailyPriceOHLCS, StockTimeframe.WEEKLY)); // some files might end with empty line -> 7 for good measure instead of 5
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // filter out spillover data from previous weeks, or from files that end in empty line
        LocalDate last2Sunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY)).with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        LocalDate lastSunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        LocalDate Jan1st = LocalDate.of(2024, 1, 1);
        LocalDate Jun22nd = LocalDate.of(2024, 6, 30);
        LocalDate Jul4th = LocalDate.of(2024, 7, 4);
        LocalDate afterDate = Jan1st;
        List<DailyPriceOHLC> lastWeekDailyPricesFinal = new ArrayList<>(lastWeekDailyPrices.stream()
                .filter(dailyPriceOHLC -> dailyPriceOHLC.getDate().isAfter(afterDate)) //&& dailyPriceOHLC.getDate().isAfter(Jun22nd))
                .toList());
//        List<WeeklyPriceOHLC> lastWeekPricesFinal = lastWeekPrices.stream()
//                .map(WeeklyPriceOHLC.class::cast)
//                .filter(dailyPriceOHLC -> dailyPriceOHLC.getStartDate().isAfter(lastSunday))
//                .toList();

//        partitionDataAndSave(lastWeekPricesFinal, pricesOhlcRepository);
//        log.info("max date {}", lastWeekDailyPricesFinal.stream().max(Comparator.comparing(DailyPriceOHLC::getDate)));
//        log.info("min date {}", lastWeekDailyPricesFinal.stream().min(Comparator.comparing(DailyPriceOHLC::getDate)));

//        Map<LocalDate, List<String>> tickersByDate = lastWeekDailyPricesFinal.stream()
//                .collect(Collectors.groupingBy(
//                                DailyPriceOHLC::getDate,
//                                Collectors.mapping(DailyPriceOHLC::getTicker, Collectors.toList())
//                        )
//                );
        long dailyPricesPriorJan1st2024 = dailyPricesOHLCRepository.countByDateAfter(afterDate);
        List<DailyPriceOHLC> dailyPricesYTD = dailyPricesOHLCRepository.findByDateAfter(afterDate);
        Map<String, List<LocalDate>> dailyPricesYTDGroupByTickerAndDate = dailyPricesYTD.stream()
                .collect(Collectors.groupingBy(
                                DailyPriceOHLC::getTicker,
                                Collectors.mapping(DailyPriceOHLC::getDate, Collectors.toList())
                        )
                );

        log.info("dailyPricesPriorJan1st2024 : {}", dailyPricesPriorJan1st2024);
        log.info("lastWeekDailyPricesFinal size {}", lastWeekDailyPricesFinal.size());
//        lastWeekDailyPricesFinal = lastWeekDailyPricesFinal.stream().filter(dailyPriceOHLC -> !dailyPricesYTDGroupByTickerAndDate.get(dailyPriceOHLC.getTicker()).contains(dailyPriceOHLC.getDate())).toList();

//        log.info("lastWeekDailyPricesFinal after removing duplicates {}", lastWeekDailyPricesFinal.size());


//        Map<LocalDate, List<String>> tickersByDate = lastWeekDailyPricesFinal.stream()
//                .collect(Collectors.groupingBy(
//                                DailyPriceOHLC::getDate,
//                                Collectors.mapping(DailyPriceOHLC::getTicker, Collectors.toList())
//                        )
//                );
//        log.info("final tickersByDate : {}", tickersByDate);
//        partitionDataAndSave(lastWeekDailyPricesFinal, pricesOhlcRepository);

//        log.info("daily prices after Jan 1st 2024 in DB: 705832");

        refreshMaterializedViewsRepository.refreshWeeklyPrices();
        refreshMaterializedViewsRepository.refreshMonthlyPrices();
        refreshMaterializedViewsRepository.refreshYearlyPrices();
    }

    @Transactional
    public void saveDailyPricesFromFiles() {
        List<AbstractPriceOHLC> ohlcList = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        ohlcList.addAll(dailyPricesOHLCFromFile(srcFile));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<DailyPriceOHLC> dailyOHLCList = ohlcList.stream()
                .map(DailyPriceOHLC.class::cast)
                .toList();
        log.info("OPEN_IS_ZERO_ERROR {} problems", OPEN_IS_ZERO_ERROR.get());
        log.info("HIGH_LOW_ERROR {} problems", HIGH_LOW_ERROR.get());
        log.info("ohlcList size {}", dailyOHLCList.size());
        partitionDataAndSave(dailyOHLCList, pricesOhlcRepository);
    }

    @Transactional
    public void savePricesForTimeframe(StockTimeframe stockTimeframe) {
        List<? extends AbstractPriceOHLC> pricesOHLCs = pricesOHLCForTimeframe(stockTimeframe);
        partitionDataAndSave(pricesOHLCs, pricesOhlcRepository);
    }


}