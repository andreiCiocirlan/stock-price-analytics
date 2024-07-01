package com.example.stockprices.service;

import com.example.stockprices.model.prices.Stock;
import com.example.stockprices.repository.StockRepository;
import com.example.stockprices.util.FileUtils;
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
import static java.nio.file.Files.walk;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional
    public void saveStocks() throws IOException {
        List<String> tickersXTB = FileUtils.readTickersXTB().stream().map(s -> s.concat(".csv")).toList();
        List<Stock> stocks = new ArrayList<>();
        try (Stream<Path> walk = walk(Paths.get(STOCKS_LOCATION))) {
            walk.filter(Files::isRegularFile)
                .parallel().forEachOrdered(srcFile -> { // must be forEachOrdered
                    String fileName = srcFile.getFileName().toString();
                    String ticker = fileName.substring(0, fileName.length() - 4);
                    if (tickersXTB.contains(fileName)) {
                        stocks.add(new Stock(ticker, LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)), true));
                    } else {
                        stocks.add(new Stock(ticker, LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))));
                    }
                });
        }
        stocks.removeAll(stockRepository.findAll());
        log.info("remaining stocks {}", stocks);
        stockRepository.saveAll(stocks);
    }

}
