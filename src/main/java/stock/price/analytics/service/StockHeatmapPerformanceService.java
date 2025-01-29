package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    private final StockService stockService;

    public List<StockPerformanceDTO> stockPerformanceForDateAndTimeframeAndFilters(
            StockTimeframe timeFrame, Boolean positivePerfFirst, Integer limit, Double cfdMargin, List<String> tickers) {
        List<Stock> result = stockService.stocksCacheMap().values().stream()
                .filter(stockFilterPredicate(tickers, cfdMargin))
                .toList();

        List<StockPerformanceDTO> performanceDTOs = result.stream()
                .map(stock -> new StockPerformanceDTO(stock.getTicker(), stock.performanceFor(timeFrame)))
                .sorted(Comparator.comparingDouble(StockPerformanceDTO::performance))
                .toList();

        if (Boolean.TRUE.equals(positivePerfFirst)) {
            performanceDTOs = performanceDTOs.stream()
                    .sorted(Comparator.comparingDouble(StockPerformanceDTO::performance).reversed())
                    .collect(Collectors.toList());
        }
        if (limit != null) {
            performanceDTOs = performanceDTOs.subList(0, Math.min(limit, performanceDTOs.size()));
        }

        return performanceDTOs;
    }

    private Predicate<? super Stock> stockFilterPredicate(List<String> tickers, Double cfdMargin) {
        return stock -> (cfdMargin == null || stock.getCfdMargin() == cfdMargin) &&
                (tickers.isEmpty() || tickers.contains(stock.getTicker()));
    }

}