package stock.price.analytics.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.repository.PricesOHLCRepository;
import stock.price.analytics.repository.RefreshMaterializedViewsRepository;
import stock.price.analytics.util.Constants;

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

import static java.nio.file.Files.walk;
import static stock.price.analytics.util.PriceEntityPartitionAndSaveUtil.partitionDataAndSave;
import static stock.price.analytics.util.PricesOHLCUtil.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoricalPricesService {

    private final PricesOHLCRepository pricesOhlcRepository;
    private final RefreshMaterializedViewsRepository refreshMaterializedViewsRepository;

    private static List<? extends AbstractPriceOHLC> pricesOHLCForTimeframe(StockTimeframe stockTimeframe) {
        List<AbstractPriceOHLC> priceOHLCS = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        try {
                            priceOHLCS.addAll(pricesOHLCForFileAndTimeframe(srcFile, stockTimeframe));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
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
    public void saveLastWeekPricesFromFiles() {
        List<DailyPriceOHLC> lastWeekDailyPrices = new ArrayList<>();
        List<AbstractPriceOHLC> lastWeekPrices = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        try {
                            List<DailyPriceOHLC> dailyPriceOHLCS = dailyPricesFromFileLastWeek(srcFile, 7);
                            lastWeekDailyPrices.addAll(dailyPriceOHLCS); // some files might end with empty line -> 7 for good measure instead of 5
                            lastWeekPrices.addAll(getPriceOHLCsForTimeframe(dailyPriceOHLCS, StockTimeframe.WEEKLY)); // some files might end with empty line -> 7 for good measure instead of 5
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // filter out spillover data from previous weeks, or from files that end in empty line
        LocalDate lastSunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        List<DailyPriceOHLC> lastWeekDailyPricesFinal = lastWeekDailyPrices.stream()
                .filter(dailyPriceOHLC -> dailyPriceOHLC.getDate().isAfter(lastSunday))
                .toList();
        List<WeeklyPriceOHLC> lastWeekPricesFinal = lastWeekPrices.stream()
                .map(WeeklyPriceOHLC.class::cast)
                .filter(dailyPriceOHLC -> dailyPriceOHLC.getStartDate().isAfter(lastSunday))
                .toList();

        partitionDataAndSave(lastWeekDailyPricesFinal, pricesOhlcRepository);
        partitionDataAndSave(lastWeekPricesFinal, pricesOhlcRepository);
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
                        try {
                            ohlcList.addAll(dailyPricesOHLCFromFile(srcFile));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
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