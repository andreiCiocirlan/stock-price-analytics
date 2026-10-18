package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.dto.StdDevProjectionDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.StdDevProjectionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projections")
public class StdDevProjectionController {

    private final StdDevProjectionService stdDevProjectionService;

    @GetMapping("/confluence/{ticker}")
    public List<StdDevProjectionDTO> getConfluenceProjections(
            @PathVariable String ticker,
            @RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return stdDevProjectionService.getConfluenceProjections(ticker.toUpperCase(), timeframe);
    }

}
