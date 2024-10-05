package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.stocks.StockRepository;
import stock.price.analytics.service.StockService;

import java.io.IOException;
import java.util.List;

import static stock.price.analytics.util.TradingDateUtil.tradingDateNow;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockRepository stockRepository;

    @PostMapping("/save_all")
    @ResponseStatus(HttpStatus.OK)
    public void saveStocksInDB() throws IOException {
        stockService.saveStocks();
    }

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


}
