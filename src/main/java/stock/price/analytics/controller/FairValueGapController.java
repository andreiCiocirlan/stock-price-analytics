package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.FairValueGapService;

import java.util.List;

@RequestMapping("/fvg")
@RestController
@RequiredArgsConstructor
public class FairValueGapController {

    private final FairValueGapService fairValueGapService;

    @GetMapping("/find-new")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findNewFVGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return fairValueGapService.findNewFVGsFor(timeframe);
    }

    @PostMapping("/find-new-and-save")
    @ResponseStatus(HttpStatus.CREATED)
    public void findNewFVGsAndSaveFor(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        fairValueGapService.findNewFVGsAndSaveFor(timeframe);
    }

    @GetMapping("/find-closed")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findClosedVsGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return fairValueGapService.findClosedFVGsFor(timeframe);
    }

    @PutMapping("/update-closed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateClosedFVGsFor(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        fairValueGapService.updateClosedFVGsFor(timeframe);
    }
}