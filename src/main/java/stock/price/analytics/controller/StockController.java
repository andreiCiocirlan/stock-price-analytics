package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.model.prices.Stock;
import stock.price.analytics.repository.StockRepository;
import stock.price.analytics.service.StockService;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockRepository stockRepository;

    @PostMapping("/save_stocks")
    @ResponseStatus(HttpStatus.OK)
    public void saveStocksInDB() throws IOException {
        stockService.saveStocks();
    }

    @GetMapping("/stocks")
    @ResponseStatus(HttpStatus.OK)
    public List<Stock> getStocks() {
        return stockRepository.findAll();
    }

}
