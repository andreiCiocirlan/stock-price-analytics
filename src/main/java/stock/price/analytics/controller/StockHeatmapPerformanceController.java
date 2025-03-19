package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import stock.price.analytics.controller.dto.StockPerformanceDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.stocks.enums.MarketState;
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

    @GetMapping("/stock-performance-json")
    @ResponseBody
    public List<StockPerformanceDTO> getStockPerformance(@RequestParam(required = false, value = "timeFrame") String timeFrame,
                                                         @RequestParam(required = false, value = "positivePerfFirst") Boolean positivePerfFirst,
                                                         @RequestParam(required = false, value = "limit") Integer limit,
                                                         @RequestParam(required = false, value = "cfdMargin") List<Double> cfdMargins,
                                                         @RequestParam(required = false, value = "priceMilestone") String priceMilestone,
                                                         @RequestParam(required = false, value = "milestoneType") String milestoneType,
                                                         @RequestParam(required = false, value = "marketState") MarketState marketState) {
        StockTimeframe stockTimeframe = ("undefined".equals(timeFrame)) ? StockTimeframe.MONTHLY : StockTimeframe.valueOf(timeFrame);
        List<String> tickers = emptyList();
        if (!isNoneMilestoneType(milestoneType)) {
            tickers = priceMilestoneService.findTickersForMilestone(priceMilestone, milestoneType, cfdMargins);
            if (tickers.isEmpty()) {
                return emptyList();
            }
        }
        return stockHeatmapPerformanceService.stockPerformanceFor(stockTimeframe, positivePerfFirst, limit, cfdMargins, tickers, marketState);
    }

    private boolean isNoneMilestoneType(String milestoneType) {
        return "none".equals(milestoneType);
    }

}