package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    public void dbToJSONFile() {
        dailyPricesJSONService.exportDailyPricesToJson(LocalDate.of(2025, 1, 22));
    }

}