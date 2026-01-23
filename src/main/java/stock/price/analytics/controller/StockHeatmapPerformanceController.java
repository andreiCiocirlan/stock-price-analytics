package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import stock.price.analytics.model.dto.StockHeatmapRequest;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.service.StockHeatmapPerformanceService;

import java.util.List;

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