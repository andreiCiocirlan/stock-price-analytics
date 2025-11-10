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

    @GetMapping("/stock-performance")
    @ResponseBody
    public ModelAndView getStockPerformanceView(@RequestParam(required = false, value = "timeFrame") String timeFrame) {
        return new ModelAndView("stock-performance");
    }

    @PostMapping("/stock-performance-json")
    @ResponseBody
    public List<StockPerformanceDTO> getStockPerformance(@RequestBody StockHeatmapRequest request) {
        return stockHeatmapPerformanceService.stockPerformanceFor(request);

    }

}