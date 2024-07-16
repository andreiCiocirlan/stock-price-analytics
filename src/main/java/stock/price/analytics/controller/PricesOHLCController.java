package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.controller.dto.CandleOHLCWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PriceOHLCService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @PostMapping("/update-higher-timeframes")
    public void updateHigherTimeframesForDate(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        priceOHLCService.updateHigherTimeframesPricesFor(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}