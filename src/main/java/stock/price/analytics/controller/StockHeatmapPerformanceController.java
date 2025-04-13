package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import stock.price.analytics.controller.dto.StockHeatmapRequest;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceMilestoneService;
import stock.price.analytics.service.StockHeatmapPerformanceService;

import java.util.List;

import static java.util.Collections.emptyList;

@Controller
@RequiredArgsConstructor
public class StockHeatmapPerformanceController {

    private final StockHeatmapPerformanceService stockHeatmapPerformanceService;
    private final PriceMilestoneService priceMilestoneService;

    @GetMapping("/stock-performance")
    @ResponseBody
    public ModelAndView getStockPerformanceView(@RequestParam(required = false, value = "timeFrame") String timeFrame) {
        return new ModelAndView("stock-performance");
    }

    @PostMapping("/stock-performance-json")
    @ResponseBody
    public List<StockPerformanceDTO> getStockPerformance(@RequestBody StockHeatmapRequest request) {
        StockTimeframe stockTimeframe = ("undefined".equals(request.getTimeFrame())) ? StockTimeframe.MONTHLY : StockTimeframe.valueOf(request.getTimeFrame());
        List<String> tickers = emptyList();
        if (!request.getMilestoneTypes().isEmpty()) {
            tickers = priceMilestoneService.findTickersForMilestones(request.getPriceMilestones(), request.getMilestoneTypes(), request.getCfdMargins());

            if (tickers.isEmpty()) {
                return emptyList();
            }
        }
        return stockHeatmapPerformanceService.stockPerformanceFor(
                stockTimeframe,
                request.getPositivePerfFirst(),
                request.getLimit(),
                request.getCfdMargins(),
                tickers,
                request.getMarketState()
        );
    }

}