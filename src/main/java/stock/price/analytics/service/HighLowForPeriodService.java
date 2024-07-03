package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.PriceEntity;
import stock.price.analytics.model.prices.enums.StockPerformanceInterval;
import stock.price.analytics.model.prices.highlow.HighLow30Days;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.repository.HighLowRepository;
import stock.price.analytics.util.Constants;
import stock.price.analytics.util.FileUtils;
import stock.price.analytics.util.HighLowPeriodPricesUtil;
import stock.price.analytics.util.PriceEntityPartitionAndSaveUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.Files.walk;
import static java.time.LocalDate.of;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {

    private static final LocalDate START_DATE = of(2022, 6, 1);
    private static final LocalDate END_DATE = of(2025, 6, 1);
    private final HighLowRepository highLowRepository;

    @Transactional
    public void saveHighLowPricesForPeriod(StockPerformanceInterval stockPerformanceInterval) throws IOException {
        List<? extends PriceEntity> highLowPrices = processForInterval(stockPerformanceInterval);
        PriceEntityPartitionAndSaveUtil.partitionDataAndSave(highLowPrices, highLowRepository);
    }

    private static List<? extends PriceEntity>  processForInterval(StockPerformanceInterval stockPerformanceInterval) throws IOException {
        List<String> tickersXTB = FileUtils.readTickersXTB().stream().map(s -> s.concat(".csv")).toList();
        List<HighLowForPeriod> highLowForPeriod = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> {
                        try {
//                            if (tickersXTB.contains(srcFile.getFileName().toString())) {
                            if ("MBLY.csv".contains(srcFile.getFileName().toString())) {
                                highLowForPeriod.addAll(HighLowPeriodPricesUtil.highLowFromFileForPeriod(srcFile, START_DATE, END_DATE, stockPerformanceInterval));

//                                if (StockPerformanceInterval.STOCK_PERF_INTERVAL_30D == stockPerformanceInterval) {
//                                    highLowForPeriod.addAll(hhighLowFromFileForPeriod(srcFile, START_DATE, END_DATE, StockPerformanceInterval.STOCK_PERF_INTERVAL_30D));
//                                } else if (StockPerformanceInterval.STOCK_PERF_INTERVAL_52W == stockPerformanceInterval) {
//                                    log.info("STOCK_PERF_INTERVAL_52W");
//                                    highLowForPeriod.addAll(hhighLowFromFileForPeriod(srcFile, START_DATE, END_DATE, StockPerformanceInterval.STOCK_PERF_INTERVAL_52W));
//                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return switch (stockPerformanceInterval) {
            case STOCK_PERF_INTERVAL_30D -> highLowForPeriod.stream()
                    .map(HighLow30Days.class::cast)
                    .toList();
            case STOCK_PERF_INTERVAL_52W -> highLowForPeriod.stream()
                    .map(HighLow52Week.class::cast)
                    .toList();
        } ;
    }

}
