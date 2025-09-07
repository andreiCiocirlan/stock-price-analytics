package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    private final CacheService cacheService;

    public List<StockPerformanceDTO> stockPerformanceFor(StockTimeframe timeFrame, Boolean positivePerfFirst, Integer limit, List<Double> cfdMargins, List<String> tickers) {
        Collection<DailyPrice> latestPrices = cacheService.getCachedDailyPrices().stream()
                .collect(Collectors.toMap(DailyPrice::getTicker, Function.identity(), BinaryOperator.maxBy(Comparator.comparing(DailyPrice::getDate)))).values();

        Map<String, StockPerformanceDTO> dailyPricesCache = latestPrices.stream()
                .filter(dp -> tickers.contains(dp.getTicker()))
                .map(dp -> new StockPerformanceDTO(dp.getTicker(), dp.getPerformance()))
                .collect(Collectors.toMap(StockPerformanceDTO::ticker, dto -> dto));

        List<StockPerformanceDTO> result = new ArrayList<>();
        cacheService.getStocksMap().values().stream()
                .filter(stockFilterPredicate(tickers, cfdMargins))
                .forEach(stock -> result.add(new StockPerformanceDTO(stock.getTicker(), stock.performanceFor(timeFrame))));

        List<StockPerformanceDTO> performanceDTOs = result.stream()
                .sorted(Comparator.comparingDouble(StockPerformanceDTO::performance)
                        .thenComparing(StockPerformanceDTO::ticker))
                .toList();

        if (Boolean.TRUE.equals(positivePerfFirst)) {
            performanceDTOs = performanceDTOs.stream()
                    .sorted(Comparator.comparingDouble(StockPerformanceDTO::performance).reversed()
                            .thenComparing(StockPerformanceDTO::ticker))
                    .collect(Collectors.toList());
        }
        if (limit != null) {
            performanceDTOs = performanceDTOs.subList(0, Math.min(limit, performanceDTOs.size()));
        }

        return performanceDTOs;
    }

    private Predicate<? super Stock> stockFilterPredicate(List<String> tickers, List<Double> cfdMargins) {
        return stock -> (cfdMargins.isEmpty() || cfdMargins.contains(stock.getCfdMargin())) &&
                        (tickers.isEmpty() || tickers.contains(stock.getTicker()));
    }

}