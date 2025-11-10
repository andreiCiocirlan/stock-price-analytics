package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.dto.StockHeatmapRequest;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    private final CacheService cacheService;
    private final CandleStickService candleStickService;
    private final PriceMilestoneService priceMilestoneService;

    public List<StockPerformanceDTO> stockPerformanceFor(StockHeatmapRequest request) {
        StockTimeframe stockTimeframe = ("undefined".equals(request.timeFrame())) ? StockTimeframe.MONTHLY : StockTimeframe.valueOf(request.timeFrame());
        List<String> tickers = emptyList();
        if (request.hasMilestonesOrCandlestickFilters()) {
            tickers = priceMilestoneService.tickersFor(request, stockTimeframe);

            if (tickers.isEmpty()) {
                return emptyList();
            }
        }
        if (request.hasTradingIdea()) {
            String cfdMargins = request.cfdMargins().stream().map(cfdMargin -> STR."'\{cfdMargin}'").collect(Collectors.joining(", "));
            List<String> tradingIdeaTickers = switch (request.tradingIdea()) {
                case COMPRESSED_PRICE -> candleStickService.compressedPriceFor(stockTimeframe, cfdMargins);
                default -> emptyList();
            };

            if (tradingIdeaTickers.isEmpty()) {
                return emptyList();
            }

            if (!tickers.isEmpty()) {
                tickers = new ArrayList<>(tickers);
                tickers.retainAll(tradingIdeaTickers);
                if (tickers.isEmpty()) {
                    return emptyList();
                }
            } else {
                tickers = tradingIdeaTickers;
            }
        }

        return getStockPerformance(
                stockTimeframe,
                request.positivePerfFirst(),
                request.limit(),
                request.cfdMargins(),
                tickers
        );
    }

    public List<StockPerformanceDTO> getStockPerformance(StockTimeframe timeFrame, Boolean positivePerfFirst, Integer limit, List<Double> cfdMargins, List<String> tickers) {
        List<StockPerformanceDTO> result = new ArrayList<>();
        cacheService.getStocksMap().values().stream()
                .filter(s -> (cfdMargins.isEmpty() || cfdMargins.contains(s.getCfdMargin())) &&
                             (tickers.isEmpty() || tickers.contains(s.getTicker())))
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

}