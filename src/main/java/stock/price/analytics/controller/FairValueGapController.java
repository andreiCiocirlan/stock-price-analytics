package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.FairValueGapService;

import java.util.List;

import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X;
import static stock.price.analytics.util.LoggingUtil.logTimeAndReturn;

@RequestMapping("/fvg")
@RestController
@RequiredArgsConstructor
public class FairValueGapController {

    private final FairValueGapService fairValueGapService;
    private final CacheService cacheService;

    @GetMapping("/find-new")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findNewFVGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return fairValueGapService.findNewFVGsFor(cacheService.getCachedTickers(), timeframe, false);
    }

    @PostMapping("/find-new-and-save")
    @ResponseStatus(HttpStatus.CREATED)
    public void findNewFVGsAndSaveFor(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        if (timeframe == null) { // find new FVGs and save for all timeframes
            fairValueGapService.findNewFVGsAndSaveForAllTimeframes(cacheService.getCachedTickers(), false);
        } else {
            fairValueGapService.findNewFVGsAndSaveFor(cacheService.getCachedTickers(), timeframe, false);
        }
    }

    @PostMapping("/save-new-and-update-hl-and-closed")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveNewFVGsAndUpdateHighLowAndClosed(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        if (timeframe == null) { // save new and update for all timeframes
            fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
        } else {
            fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedFor(timeframe);
        }
    }

    @GetMapping("/find-update-high-low-and-closed")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findUpdatedFVGsHighLowAndClosedFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return logTimeAndReturn(() -> fairValueGapService.findUpdatedFVGsHighLowAndClosedFor(timeframe, false), "Searching " + timeframe + " FVGs to Update");
    }

    @PutMapping("/update-high-low-and-closed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFVGsHighLowAndClosedFor(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        if (timeframe == null) { // update closed for all timeframes
            fairValueGapService.updateFVGsHighLowAndClosedForAllTimeframes(false);
        } else {
            fairValueGapService.updateFVGsHighLowAndClosedFor(timeframe, false);
        }
    }

    @GetMapping("/alert-fvg-tagged-95th-percentile")
    @ResponseStatus(HttpStatus.OK)
    public void findUpdatedFVGsHighLowAndClosedFor() {
        fairValueGapService.logFVGsTagged95thPercentile(CFD_MARGINS_5X_4X);
    }
}