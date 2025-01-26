package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.FairValueGapService;

@RequestMapping("/fvg")
@RestController
@RequiredArgsConstructor
public class FairValueGapController {

    private final FairValueGapService fairValueGapService;

    @PostMapping("/find-and-save")
    @ResponseStatus(HttpStatus.CREATED)
    public void findAndSaveFVGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        fairValueGapService.findAndSaveFVGsFor(timeframe);
    }
}