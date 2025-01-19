package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.FairValueGapService;

@RequestMapping("/fvg")
@RestController
@RequiredArgsConstructor
public class FairValueGapController {

    private final FairValueGapService fairValueGapService;

    @PostMapping("/find-and-save")
    public void findAndSaveFVGsFor(@RequestParam(value = "timeframe") String timeframe) {
        fairValueGapService.findAndSaveFVGsFor(timeframe);
    }
}