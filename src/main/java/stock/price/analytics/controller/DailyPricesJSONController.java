package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.service.DailyPricesJSONService;

import java.time.LocalDate;

@RequestMapping("/daily-prices-json")
@RestController
@RequiredArgsConstructor
public class DailyPricesJSONController {

    private final DailyPricesJSONService dailyPricesJSONService;

    @PostMapping("/db-to-json-file")
    @ResponseStatus(HttpStatus.CREATED)
    public void dbToJSONFile(@RequestParam(name = "tradingDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradingDate) {
        dailyPricesJSONService.exportDailyPricesToJson(tradingDate);
    }

    @PostMapping("/save-json-from-file")
    public void saveDailyPricesJSONFrom(@RequestParam("fileName") String fileName) {
        dailyPricesJSONService.saveDailyPricesJSONFrom(fileName);
    }

}