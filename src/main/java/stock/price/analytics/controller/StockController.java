package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.StockService;
import stock.price.analytics.service.TickerRenameService;

import java.util.List;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockRepository stockRepository;
    private final TickerRenameService tickerRenameService;

    @PostMapping("/save_stock")
    public void saveStockInDB(@RequestParam(value = "tickers") String tickers,
                              @RequestParam(value = "cfdMargin") Double cfdMargin,
                              @RequestParam(value = "xtbStock") Boolean xtbStock,
                              @RequestParam(value = "shortSell") Boolean shortSell) {
        double xtb_cfdMargin = cfdMargin != null ? cfdMargin : 0d;
        stockService.saveStocks(tickers, Boolean.TRUE.equals(xtbStock), Boolean.TRUE.equals(shortSell), xtb_cfdMargin);
    }

    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<Stock> getStocks() {
        return stockRepository.findAll();
    }

    @PutMapping("/update-high-low")
    public void updateStocksHighLow() {
        stockService.updateStocksHighLow(tradingDateNow());
    }

    @PutMapping("/rename-ticker")
    public void renameTicker(@RequestParam(value = "oldTicker") String oldTicker, @RequestParam(value = "newTicker") String newTicker) {
        tickerRenameService.renameTicker(oldTicker, newTicker);
    }

}
