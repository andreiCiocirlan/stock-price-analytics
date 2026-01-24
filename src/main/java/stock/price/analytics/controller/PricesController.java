package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.model.dto.CandleWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.ohlc.DailyPriceRepository;
import stock.price.analytics.service.AsyncPersistenceService;
import stock.price.analytics.service.PriceService;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.util.LoggingUtil.logTime;

@RequestMapping("/ohlc")
@RestController
@RequiredArgsConstructor
public class PricesController {

    private final PriceService priceService;
    private final DailyPriceRepository dailyPriceRepository;
    private final AsyncPersistenceService asyncPersistenceService;

    @GetMapping("/prices")
    public List<CandleWithDateDTO> pricesFor(@RequestParam("ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return priceService.findFor(ticker, StockTimeframe.valueOf(timeFrame.toUpperCase()));
    }

    @PostMapping("/persist-test")
    public void simulatePricesPersistence(@RequestParam(name = "tradingDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate tradingDate) {
        List<DailyPrice> dailyImportedPrices = dailyPriceRepository.findByDate(tradingDate);
        asyncPersistenceService.partitionDataAndSaveWithLogTime(dailyImportedPrices, dailyPriceRepository, "saved " + dailyImportedPrices.size() + " prices");
        logTime(() -> priceService.updateAllTimeframePrices(dailyImportedPrices), "updated prices for all timeframes");
    }

}