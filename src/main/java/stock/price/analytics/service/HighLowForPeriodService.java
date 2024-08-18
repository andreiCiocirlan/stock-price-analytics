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
import stock.price.analytics.repository.prices.HighLowForPeriodRepository;
import stock.price.analytics.util.Constants;

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
import static stock.price.analytics.util.HighLowPeriodPricesUtil.highLowFromFileForPeriod;
import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {

    private static final LocalDate START_DATE = of(2022, 6, 1);
    private static final LocalDate END_DATE = of(2025, 6, 1);
    private final HighLowForPeriodRepository highLowForPeriodRepository;

    @Transactional
    public void saveHighLowPricesForPeriod(StockPerformanceInterval stockPerformanceInterval) {
        List<HighLowForPeriod> highLowForPeriod = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(Constants.STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> {
                        try {
                            highLowForPeriod.addAll(highLowFromFileForPeriod(srcFile, START_DATE, END_DATE, stockPerformanceInterval));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<? extends PriceEntity> highLowPrices = switch (stockPerformanceInterval) {
            case STOCK_PERF_INTERVAL_30D -> highLowForPeriod.stream()
                    .map(HighLow30Days.class::cast)
                    .toList();
            case STOCK_PERF_INTERVAL_52W -> highLowForPeriod.stream()
                    .map(HighLow52Week.class::cast)
                    .toList();
        };
        partitionDataAndSave(highLowPrices, highLowForPeriodRepository);
    }

}
