package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.model.stocks.enums.MarketState;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    private final CacheService cacheService;

    private static void addPreMarketPriceOrStockPrice(Stock stock, MarketState marketState, StockTimeframe timeFrame, Map<String, StockPerformanceDTO> preMarketMap, List<StockPerformanceDTO> result) {
        String ticker = stock.getTicker();
        StockPerformanceDTO preMarketPrice = preMarketMap.get(ticker);

        if (marketState == MarketState.PRE) {
            // If the ticker is found in preMarketPrices, use that; otherwise, use the stock's performance
            result.add(Objects.requireNonNullElseGet(preMarketPrice, () -> new StockPerformanceDTO(ticker, stock.performanceFor(timeFrame))));
        } else {
            result.add(new StockPerformanceDTO(ticker, stock.performanceFor(timeFrame)));
        }
    }

    public List<StockPerformanceDTO> stockPerformanceFor(StockTimeframe timeFrame, Boolean positivePerfFirst, Integer limit, List<Double> cfdMargins, List<String> tickers, MarketState marketState) {
        Map<String, StockPerformanceDTO> dailyPricesCache = cacheService.getCachedDailyPrices(marketState).stream()
                .filter(dp -> tickers.contains(dp.getTicker()))
                .map(dp -> new StockPerformanceDTO(dp.getTicker(), dp.getPerformance()))
                .collect(Collectors.toMap(StockPerformanceDTO::ticker, dto -> dto));

        List<StockPerformanceDTO> result = new ArrayList<>();
        cacheService.getStocksMap().values().stream()
                .filter(stockFilterPredicate(tickers, cfdMargins))
                .forEach(stock -> addPreMarketPriceOrStockPrice(stock, marketState, timeFrame, dailyPricesCache, result)); // pre-market price takes precedence

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