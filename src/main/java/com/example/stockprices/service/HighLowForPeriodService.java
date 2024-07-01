package com.example.stockprices.service;

import com.example.stockprices.model.prices.PriceEntity;
import com.example.stockprices.model.prices.enums.StockPerformanceInterval;
import com.example.stockprices.model.prices.highlow.HighLow30Days;
import com.example.stockprices.model.prices.highlow.HighLow52Week;
import com.example.stockprices.model.prices.highlow.HighLowForPeriod;
import com.example.stockprices.repository.HighLowRepository;
import com.example.stockprices.util.FileUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.example.stockprices.util.Constants.STOCKS_LOCATION;
import static com.example.stockprices.util.HighLowPeriodPricesUtil.highLow30DaysFromFile;
import static com.example.stockprices.util.HighLowPeriodPricesUtil.highLow52WeeksFromFile;
import static com.example.stockprices.util.PriceEntityPartitionAndSaveUtil.partitionDataAndSave;
import static java.nio.file.Files.walk;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {
    private final HighLowRepository highLowRepository;

    @Transactional
    public void saveHighLowPricesForPeriod(LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) throws IOException {
        List<String> tickersXTB = FileUtils.readTickersXTB().stream().map(s -> s.concat(".csv")).toList();
        partitionDataAndSave(processForInterval(tickersXTB, startDate, endDate, stockPerformanceInterval), highLowRepository);
    }

    private static List<? extends PriceEntity>  processForInterval(List<String> tickersXTB, LocalDate startDate, LocalDate endDate, StockPerformanceInterval stockPerformanceInterval) {
        List<HighLowForPeriod> highLowForPeriod = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                    .parallel().forEachOrdered(srcFile -> {
                        try {
                            if (!tickersXTB.contains(srcFile.getFileName().toString())) {
                                if (StockPerformanceInterval.STOCK_PERF_INTERVAL_30D == stockPerformanceInterval) {
                                    highLowForPeriod.addAll(highLow30DaysFromFile(srcFile, startDate, endDate, StockPerformanceInterval.STOCK_PERF_INTERVAL_30D));
                                } else if (StockPerformanceInterval.STOCK_PERF_INTERVAL_52W == stockPerformanceInterval) {
                                    highLowForPeriod.addAll(highLow52WeeksFromFile(srcFile, startDate, endDate, StockPerformanceInterval.STOCK_PERF_INTERVAL_52W));
                                }
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
