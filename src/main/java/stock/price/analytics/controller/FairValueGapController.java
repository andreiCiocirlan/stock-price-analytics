package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.FairValueGapService;

import java.util.List;

import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;

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

    @GetMapping("/find-update-high-low-and-closed")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findUpdatedFVGsHighLowAndClosedFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return logTimeAndReturn(() -> fairValueGapService.findUpdatedFVGsHighLowAndClosedFor(timeframe), "Found " + timeframe + " FVGs to Update");
    }

    @PutMapping("/update-high-low-and-closed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFVGsHighLowAndClosedFor(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        fairValueGapService.updateFVGsHighLowAndClosedFor(timeframe);
    }
}