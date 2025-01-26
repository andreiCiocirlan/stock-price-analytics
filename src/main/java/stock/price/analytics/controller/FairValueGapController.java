package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.fvg.FVGRepository;
import stock.price.analytics.service.FairValueGapService;

import java.util.List;

import static stock.price.analytics.util.PartitionAndSavePriceEntityUtil.partitionDataAndSave;

@RequestMapping("/fvg")
@RestController
@RequiredArgsConstructor
public class FairValueGapController {

    private final FairValueGapService fairValueGapService;
    private final FVGRepository fvgRepository;

    @PostMapping("/find-all-and-save")
    @ResponseStatus(HttpStatus.CREATED)
    public void findAndSaveFVGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        fairValueGapService.findAllFVGsAndSaveFor(timeframe);
    }

    @GetMapping("/find-new")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findNewFVsGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return fairValueGapService.findNewFVGsFor(timeframe);
    }

    @PostMapping("/find-new-and-save")
    @ResponseStatus(HttpStatus.CREATED)
    public void findNewFVsGsAndSaveFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        partitionDataAndSave(fairValueGapService.findNewFVGsFor(timeframe), fvgRepository);
    }

    @GetMapping("/find-closed")
    @ResponseStatus(HttpStatus.OK)
    public List<FairValueGap> findClosedVsGsFor(@RequestParam(value = "timeframe") StockTimeframe timeframe) {
        return fairValueGapService.findClosedFVGsFor(timeframe);
    }

    @PutMapping("/update-closed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateClosedVsGsFor(@RequestParam(value = "timeframe", required = false) StockTimeframe timeframe) {
        if (timeframe == null) { // update closed for all timeframes
            for (StockTimeframe stockTimeframe : StockTimeframe.values()) {
                partitionDataAndSave(fairValueGapService.findClosedFVGsFor(stockTimeframe), fvgRepository);
            }
        } else {
            partitionDataAndSave(fairValueGapService.findClosedFVGsFor(timeframe), fvgRepository);
        }
    }
}