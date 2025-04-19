package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.service.DeleteTickerService;
import stock.price.analytics.service.NewTickerService;

@Slf4j
@RestController
@RequestMapping("/ticker")
@RequiredArgsConstructor
public class TickerController {

    private final NewTickerService newTickerService;
    private final DeleteTickerService deleteTickerService;

    @PostMapping("/import-all-data")
    public void importAllDataFor(@RequestParam(value = "tickers") String tickers,
                                 @RequestParam(value = "cfdMargin") Double cfdMargin,
                                 @RequestParam(value = "shortSell") Boolean shortSell) {
        newTickerService.importAllDataFor(tickers, cfdMargin, shortSell);
    }

    @DeleteMapping("/delete-data-for-ticker")
    public void deleteAllDataFor(@RequestParam(value = "ticker") String ticker) {
        deleteTickerService.deleteAllDataFor(ticker);
    }
}
