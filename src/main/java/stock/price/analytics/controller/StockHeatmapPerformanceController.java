package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceMilestoneService;
import stock.price.analytics.service.StockHeatmapPerformanceService;

import java.util.Collections;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class StockHeatmapPerformanceController {

    private final StockHeatmapPerformanceService stockHeatmapPerformanceService;
    private final PriceMilestoneService priceMilestoneService;

    @GetMapping("/stock-performance")
    @ResponseBody
    public ModelAndView getStockPerformanceView(@RequestParam(required = false, value = "timeFrame") String timeFrame) {
        return new ModelAndView("stock-performance");
    }

    @GetMapping("/stock-performance-json")
    @ResponseBody
    public List<StockPerformanceDTO> getStockPerformance(@RequestParam(required = false, value = "timeFrame") String timeFrame,
                                                         @RequestParam(required = false, value = "positivePerfFirst") Boolean positivePerfFirst,
                                                         @RequestParam(required = false, value = "limit") Integer limit,
                                                         @RequestParam(required = false, value = "cfdMargin") Double cfdMargin,
                                                         @RequestParam(required = false, value = "priceMilestone") String priceMilestone) {
        StockTimeframe stockTimeframe = ("undefined".equals(timeFrame)) ? StockTimeframe.MONTHLY : StockTimeframe.valueOf(timeFrame);
        List<String> tickers = Collections.emptyList();
        if (priceMilestone != null) {
            tickers = priceMilestoneService.findTickersForMilestone(priceMilestone, cfdMargin);
            if (tickers.isEmpty()) {
                return Collections.emptyList();
            }
        }
        return stockHeatmapPerformanceService.stockPerformanceFor(stockTimeframe, positivePerfFirst, limit, cfdMargin, tickers);
    }

}