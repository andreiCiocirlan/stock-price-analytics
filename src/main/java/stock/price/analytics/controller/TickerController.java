package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.service.TickerService;

@RestController
@RequestMapping("/ticker")
@RequiredArgsConstructor
public class TickerController {

    private final TickerService tickerService;

    @PostMapping("/import-all-data")
    public void importAllDataFor(@RequestParam(value = "tickers") String tickers,
                                 @RequestParam(value = "cfdMargin") Double cfdMargin,
                                 @RequestParam(value = "shortSell") Boolean shortSell) {
        tickerService.importAllDataFor(tickers, cfdMargin, shortSell);
    }

    @DeleteMapping("/delete-data-for-ticker")
    public void deleteAllDataFor(@RequestParam(value = "ticker") String ticker) {
        tickerService.deleteAllDataFor(ticker);
    }

    @PutMapping("/rename")
    public void renameTicker(@RequestParam(value = "oldTicker") String oldTicker, @RequestParam(value = "newTicker") String newTicker) {
        tickerService.renameTicker(oldTicker, newTicker);
    }
}
