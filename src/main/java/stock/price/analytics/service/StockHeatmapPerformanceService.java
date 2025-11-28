package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.dto.StockHeatmapRequest;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockHeatmapPerformanceService {

    private final CacheService cacheService;
    private final PriceMilestoneService priceMilestoneService;
    private final CandleStickService candleStickService;
    private final FairValueGapService fairValueGapService;
    private final DemarkService demarkService;

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

    public List<StockPerformanceDTO> stockPerformanceFor(StockHeatmapRequest request) {
        StockTimeframe stockTimeframe = parseTimeframe(request.timeFrame());

        List<String> tickers = filterTickersForRequestCriteria(request, stockTimeframe);
        if (tickers.isEmpty() && (request.hasMilestonesOrCandlestickFilters() || request.hasTradingIdea())) {
            return Collections.emptyList();
        }

        return getStockPerformance(
                stockTimeframe,
                request.positivePerfFirst(),
                request.limit(),
                request.cfdMargins(),
                tickers
        );
    }

    private StockTimeframe parseTimeframe(String timeFrameStr) {
        return "undefined".equals(timeFrameStr) ? StockTimeframe.MONTHLY : StockTimeframe.valueOf(timeFrameStr);
    }

    private List<String> filterTickersForRequestCriteria(StockHeatmapRequest request, StockTimeframe stockTimeframe) {
        List<String> tickers = Collections.emptyList();

        if (request.hasMilestonesOrCandlestickFilters()) {
            tickers = priceMilestoneService.tickersFor(request, stockTimeframe);
            if (tickers.isEmpty()) {
                return Collections.emptyList();
            }
        }

        if (request.hasTradingIdea()) {
            String cfdMarginsString = request.cfdMargins().stream()
                    .map(cfdMargin -> String.format("'%s'", cfdMargin))
                    .collect(Collectors.joining(", "));

            List<String> tradingIdeaTickers = switch (request.tradingIdea()) {
                case COMPRESSED_PRICE -> candleStickService.compressedPriceFor(stockTimeframe, cfdMarginsString);
                case PRICE_INSIDE_FVG -> fairValueGapService.priceInsideFvgFor(stockTimeframe, cfdMarginsString);
                case PRICE_HL_INSIDE_FVG -> fairValueGapService.priceHLInsideFvgFor(stockTimeframe, cfdMarginsString);
                case DEMARK_8 -> demarkService.tickersForTimeframeTdAndCfdMargins(stockTimeframe, 8, cfdMarginsString);
                case DEMARK_9 -> demarkService.tickersForTimeframeTdAndCfdMargins(stockTimeframe, 9, cfdMarginsString);
                case DEMARK_13 -> demarkService.tickersForTimeframeTdAndCfdMargins(stockTimeframe, 13, cfdMarginsString);
                default -> Collections.emptyList();
            };

            if (tradingIdeaTickers.isEmpty()) {
                return Collections.emptyList();
            }

            if (!tickers.isEmpty()) {
                List<String> filteredTickers = new ArrayList<>(tickers);
                filteredTickers.retainAll(tradingIdeaTickers);
                tickers = filteredTickers.isEmpty() ? Collections.emptyList() : filteredTickers;
            } else {
                tickers = tradingIdeaTickers;
            }
        }

        return tickers;
    }
}