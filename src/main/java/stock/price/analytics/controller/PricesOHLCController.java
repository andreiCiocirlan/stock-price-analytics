package stock.price.analytics.controller;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.controller.dto.CandleOHLCWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.service.PriceOHLCService;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("/ohlc")
@RestController
@Slf4j
@RequiredArgsConstructor
public class PricesOHLCController {

    private final PriceOHLCService priceOHLCService;
    private static final String TIMEFRAME_PATTERN = "^(DAILY|WEEKLY|MONTHLY|YEARLY)$";

    @GetMapping("/prices")
    public List<CandleOHLCWithDateDTO> dailyOHLC(@RequestParam("ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return priceOHLCService.findOHLCFor(ticker, StockTimeframe.valueOf(timeFrame.toUpperCase()));
    }

    @PostMapping("/update-higher-timeframes")
    public void updateHigherTimeframesForDate(@RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                              @RequestParam(required = false, name = "timeFrame") @Pattern(regexp = TIMEFRAME_PATTERN, message = "Invalid timeframe") String timeFrame,
                                              @RequestParam(required = false, value = "tickers") String tickers) {
        String tickersQueryParam = tickers != null ? STR."'\{tickers.replace(",", "','")}'" : null;
        if (timeFrame != null) {
            priceOHLCService.updateHigherTimeframesPricesFor(date, StockTimeframe.valueOf(timeFrame.toUpperCase()), tickersQueryParam);
        } else {
            priceOHLCService.updateAllHigherTimeframesPricesForTickers(date, tickersQueryParam);
        }

    }
}