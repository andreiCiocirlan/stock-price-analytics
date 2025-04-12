package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.service.DailyPriceJSONService;

import java.time.LocalDate;

@RequestMapping("/daily-prices-json")
@RestController
@RequiredArgsConstructor
public class DailyPriceJSONController {

    private final DailyPriceJSONService dailyPriceJSONService;

    @PostMapping("/db-to-json-file")
    @ResponseStatus(HttpStatus.CREATED)
    public void dbToJSONFile(@RequestParam(name = "tradingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        dailyPriceJSONService.exportDailyPricesToJson(tradingDate);
    }

    @PostMapping("/save-json-from-file")
    public void saveDailyPriceJSONsFrom(@RequestParam("fileName") String fileName) {
        dailyPriceJSONService.saveDailyPriceJSONsFrom(fileName);
    }

}