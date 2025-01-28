package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.client.finnhub.FinnhubClient;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.service.PricesService;

import java.util.List;

@RequestMapping("/finnhub")
@RestController
@RequiredArgsConstructor
public class FinnhubController {

    private final FinnhubClient finnhubClient;
    private final PricesService pricesService;


    @GetMapping("/daily-prices")
    public DailyPriceOHLC intraDayPrices(@RequestParam("ticker") String ticker) {
        return finnhubClient.intraDayPricesFor(ticker).orElseThrow();
    }

    @Transactional
    @GetMapping("/daily-prices-all-xtb")
    public void finnhubIntraDayPricesTickersXTB() {
        List<DailyPriceOHLC> dailyPrices = finnhubClient.intraDayPricesXTB();
        pricesService.savePrices(dailyPrices);
    }

}
