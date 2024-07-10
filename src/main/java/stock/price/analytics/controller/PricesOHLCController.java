package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.controller.dto.CandleOHLCWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceOHLCService;

import java.util.List;

@RequestMapping("/ohlc")
@RestController
@Slf4j
@RequiredArgsConstructor
public class PricesOHLCController {

    private final PriceOHLCService priceOHLCService;

    @GetMapping("/prices")
    public List<CandleOHLCWithDateDTO> dailyOHLC(@RequestParam("ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return priceOHLCService.findOHLCFor(ticker, StockTimeframe.valueOf(timeFrame.toUpperCase()));
    }

}