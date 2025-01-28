package stock.price.analytics.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.controller.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.QuarterlyPriceService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static stock.price.analytics.util.Constants.HIGHER_TIMEFRAMES_PATTERN;

@RequestMapping("/ohlc")
@RestController
@Slf4j
@RequiredArgsConstructor
public class PricesController {

    private final PricesService pricesService;
    private final QuarterlyPriceService quarterlyPriceService;

    @GetMapping("/prices")
    public List<CandleWithDateDTO> pricesFor(@RequestParam("ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return pricesService.findFor(ticker, StockTimeframe.valueOf(timeFrame.toUpperCase()));
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

    @PostMapping("/save-quarterly-prices")
    public void saveAllQuarterlyPrices() {
        quarterlyPriceService.saveAllQuarterlyPrices();
    }
}