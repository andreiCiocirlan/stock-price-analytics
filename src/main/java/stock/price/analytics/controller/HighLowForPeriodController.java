package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.service.HighLowForPeriodService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/high-low")
@RequiredArgsConstructor
public class HighLowForPeriodController {

    private final HighLowForPeriodService highLowForPeriodService;

    @PostMapping("/save-all-hl-4w-52w-ticker")
    @ResponseStatus(HttpStatus.OK)
    public void saveAllHistoricalHighLowPrices(@RequestParam("ticker") String ticker,
                                                       @RequestParam(name = "tradingDate")
                                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        highLowForPeriodService.saveAllHistoricalHighLowPrices(List.of(ticker), tradingDate);
    }

}
