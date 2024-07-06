package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import stock.price.analytics.model.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.StockPerformanceService;

import java.time.LocalDate;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class StockHeatmapController {

    private final StockPerformanceService stockPerformanceService;

    @GetMapping("/stock-performance")
    @ResponseBody
    public ModelAndView getStockPerformanceView(@RequestParam(required = false, value = "timeFrame") String timeFrame) {
        return new ModelAndView("stock-performance");
    }

    @GetMapping("/stock-performance-json")
    @ResponseBody
    public List<StockPerformanceDTO> getStockPerformance(@RequestParam(required = false, value = "timeFrame") StockTimeframe timeFrame,
                                                         @RequestParam(required = false, value = "xtb") Boolean xtb,
                                                         @RequestParam(required = false, value = "cfdMargin") Double cfdMargin) {
        return stockPerformanceService.stockPerformanceForDateAndTimeframeAndFilters(
                timeFrame != null ? timeFrame : StockTimeframe.MONTHLY,
                LocalDate.of(2024, 6, 26),
                xtb,
                cfdMargin
        );
    }

}