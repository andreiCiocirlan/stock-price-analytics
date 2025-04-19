package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.NewTickerService;

@Slf4j
@RestController
@RequestMapping("/ticker")
@RequiredArgsConstructor
public class TickerController {

    private final NewTickerService newTickerService;

    @PostMapping("/import-all-data")
    public void newTickersimportAllDataFor(@RequestParam(value = "tickers") String tickers,
                                           @RequestParam(value = "cfdMargin") Double cfdMargin,
                                           @RequestParam(value = "shortSell") Boolean shortSell) {
        newTickerService.importAllDataFor(tickers, cfdMargin, shortSell);
    }

    @PostMapping("/import-from-existing-json")
    public void importFromExistingJSON(@RequestParam(value = "tickers") String tickers) {
        newTickerService.importFromExistingJSONFor(tickers);
    }
}
