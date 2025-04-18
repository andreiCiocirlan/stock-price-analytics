package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.model.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceService;

import java.util.List;

@RequestMapping("/ohlc")
@RestController
@RequiredArgsConstructor
public class PricesController {

    private final PriceService priceService;

    @GetMapping("/prices")
    public List<CandleWithDateDTO> pricesFor(@RequestParam("ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return priceService.findFor(ticker, StockTimeframe.valueOf(timeFrame.toUpperCase()));
    }

}