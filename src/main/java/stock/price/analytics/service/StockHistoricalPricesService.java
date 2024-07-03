package stock.price.analytics.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;
import stock.price.analytics.repository.StockHistoricalPricesRepository;
import stock.price.analytics.repository.StockWeeklyHistoricalPricesRepository;
import stock.price.analytics.util.Constants;
import stock.price.analytics.util.PriceEntityPartitionAndSaveUtil;
import stock.price.analytics.util.StockHistoricalPricesUtil;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHistoricalPricesService {

    private final StockWeeklyHistoricalPricesRepository stockWeeklyHistoricalPricesRepository;
    private final StockHistoricalPricesRepository stockHistoricalPricesRepository;

    @Transactional
    public void saveLastWeekPricesFromFiles() {
        List<WeeklyPriceOHLC> lastWeekPrices = new ArrayList<>();
        List<DailyPriceOHLC> lastWeekDailyPrices = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                        try {
                            if (srcFile.getFileName().toString().equals("ABBV.csv")) {
                                Pair<List<DailyPriceOHLC>, List<WeeklyPriceOHLC>> pricesFromFileLastDays =
                                        StockHistoricalPricesUtil.weeklyPricesFromFileLastDays(srcFile, 7); // some files might end with empty line -> 7 for good measure instead of 5
                                lastWeekPrices.addAll(pricesFromFileLastDays.getRight());
                                lastWeekDailyPrices.addAll(pricesFromFileLastDays.getLeft());
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
        List<WeeklyPriceOHLC> lastWeekPricesFinal = lastWeekPrices.stream().filter(stockWeeklyHistoricalPrices -> stockWeeklyHistoricalPrices.getStartDate().equals(lastMonday)).toList();
        List<DailyPriceOHLC> lastWeekDailyPricesFinal = lastWeekDailyPrices.stream().filter(stockHistoricalPrices -> stockHistoricalPrices.getDate().equals(lastMonday) || stockHistoricalPrices.getDate().isAfter(lastMonday)).toList();

        PriceEntityPartitionAndSaveUtil.partitionDataAndSave(lastWeekPricesFinal, stockWeeklyHistoricalPricesRepository);
        PriceEntityPartitionAndSaveUtil.partitionDataAndSave(lastWeekDailyPricesFinal, stockHistoricalPricesRepository);
    }

}