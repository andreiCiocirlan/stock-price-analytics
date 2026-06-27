package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceGapService;

import java.util.Arrays;

@RequiredArgsConstructor
@RestController
@RequestMapping("/price-gaps")
public class PriceGapsController {

    private final PriceGapService priceGapService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void savePriceGaps() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            priceGapService.savePriceGapsTodayFor(timeframe);
        }
    }

    @PostMapping("/historical")
    @ResponseStatus(HttpStatus.OK)
    public void saveHistoricalPriceGaps(@RequestParam(value = "tickers") String tickers) {
        priceGapService.saveHistoricalPriceGapsFor(Arrays.asList(tickers.split(",")));
    }

}
