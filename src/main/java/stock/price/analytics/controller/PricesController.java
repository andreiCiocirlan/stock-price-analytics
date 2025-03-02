package stock.price.analytics.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.controller.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.service.PricesService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static stock.price.analytics.util.Constants.HIGHER_TIMEFRAMES_PATTERN;

@RequestMapping("/ohlc")
@RestController
@RequiredArgsConstructor
public class PricesController {

    private final PricesService pricesService;

    @GetMapping("/prices")
    public List<CandleWithDateDTO> pricesFor(@RequestParam("ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return pricesService.findFor(ticker, StockTimeframe.valueOf(timeFrame.toUpperCase()));
    }

    @GetMapping("/htf-prices")
    public List<AbstractPrice> htfPricesFor(@RequestParam(required = false, value = "ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return pricesService.currentCachePricesFor(StockTimeframe.valueOf(timeFrame)).stream()
                .filter(p -> ticker == null || p.getTicker().equals(ticker))
                .toList();
    }

    @PostMapping("/update-higher-timeframes")
    public void updateHigherTimeframesForDate(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                              @RequestParam(required = false, name = "timeFrames") @Pattern(regexp = HIGHER_TIMEFRAMES_PATTERN, message = "Invalid timeframe") String timeFrames,
                                              @RequestParam(required = false, value = "tickers") String tickers) {
        String tickersQueryParam = tickers != null ? STR."'\{tickers.replace(",", "','")}'" : null;
        List<StockTimeframe> timeframes = timeFrames != null ?
                StockTimeframe.valuesFromLetters(timeFrames.toUpperCase()) : Arrays.stream(StockTimeframe.values()).toList();
        pricesService.updateHigherTimeframesPricesFor(date, timeframes, tickersQueryParam);
    }

}