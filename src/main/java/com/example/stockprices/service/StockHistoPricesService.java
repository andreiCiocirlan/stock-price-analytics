package com.example.stockprices.service;


import com.example.stockprices.model.prices.enums.StockTimeframe;
import com.example.stockprices.model.prices.ohlc.*;
import com.example.stockprices.repository.PricesOHLCRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.example.stockprices.util.Constants.STOCKS_LOCATION;
import static com.example.stockprices.util.PriceEntityPartitionAndSaveUtil.partitionDataAndSave;
import static com.example.stockprices.util.StockPriceOHLCUtil.*;
import static java.nio.file.Files.walk;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoPricesService {

    private final PricesOHLCRepository pricesOhlcRepository;

    @Transactional
    public void saveLastWeekPricesFromFiles() {
        List<DailyPriceOHLC> lastWeekDailyPrices = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        try {
                            if (!srcFile.getFileName().toString().equals("RDDT.csv")) {
                                lastWeekDailyPrices.addAll(dailyPricesFromFileLastWeek(srcFile, 7)); // some files might end with empty line -> 7 for good measure instead of 5
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // filter out spillover data from previous weeks, or from files that end in empty line
        LocalDate lastMonday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        List<DailyPriceOHLC> lastWeekDailyPricesFinal = lastWeekDailyPrices.stream().filter(dailyPriceOHLC -> dailyPriceOHLC.getDate().equals(lastMonday) || dailyPriceOHLC.getDate().isAfter(lastMonday)).toList();

        partitionDataAndSave(lastWeekDailyPricesFinal, pricesOhlcRepository);
    }

    @Transactional
    public void saveDailyPricesFromFiles() {
        List<AbstractPriceOHLC> ohlcList = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                    try {
                        if (!srcFile.getFileName().toString().equals("RDDT.csv")) {
                            ohlcList.addAll(dailyPricesOHLCFromFile(srcFile));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<? extends DailyPriceOHLC> dailyOHLCList = ohlcList.stream()
                .map(DailyPriceOHLC.class::cast)
                .toList();
        log.info("OPEN_IS_ZERO_ERROR {} problems", OPEN_IS_ZERO_ERROR.get());
        log.info("HIGH_LOW_ERROR {} problems", HIGH_LOW_ERROR.get());
        log.info("ohlcList size {}", dailyOHLCList.size());
        partitionDataAndSave(dailyOHLCList, pricesOhlcRepository);
    }

    @Transactional
    public void savePricesFromFileAndTimeframe(StockTimeframe stockTimeframe) {
        partitionDataAndSave(pricesOHLCForTimeframe(stockTimeframe), pricesOhlcRepository);
    }

    private static List<? extends AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) {
        List<AbstractPriceOHLC> pricesOhlcList = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        try {
                            if (!srcFile.getFileName().toString().equals("RDDT.csv")) {
                                pricesOhlcList.addAll(pricesOHLCFromFileAndTimeframe(srcFile, stockTimeframe));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return switch (stockTimeframe) {
            case WEEKLY -> pricesOhlcList.stream().map(WeeklyPriceOHLC.class::cast).toList();
            case MONTHLY -> pricesOhlcList.stream().map(MonthlyPriceOHLC.class::cast).toList();
            case YEARLY -> pricesOhlcList.stream().map(YearlyPriceOHLC.class::cast).toList();
        };
    }


}