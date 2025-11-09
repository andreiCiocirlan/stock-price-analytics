package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import stock.price.analytics.model.dto.StockHeatmapRequest;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.CandleStickService;
import stock.price.analytics.service.PriceMilestoneService;
import stock.price.analytics.service.StockHeatmapPerformanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Controller
@RequiredArgsConstructor
public class StockHeatmapPerformanceController {

    private final StockHeatmapPerformanceService stockHeatmapPerformanceService;
    private final PriceMilestoneService priceMilestoneService;
    private final CandleStickService candleStickService;

    @GetMapping("/stock-performance")
    @ResponseBody
    public ModelAndView getStockPerformanceView(@RequestParam(required = false, value = "timeFrame") String timeFrame) {
        return new ModelAndView("stock-performance");
    }

    @PostMapping("/stock-performance-json")
    @ResponseBody
    public List<StockPerformanceDTO> getStockPerformance(@RequestBody StockHeatmapRequest request) {
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

        return stockHeatmapPerformanceService.stockPerformanceFor(
                stockTimeframe,
                request.positivePerfFirst(),
                request.limit(),
                request.cfdMargins(),
                tickers
        );
    }

}