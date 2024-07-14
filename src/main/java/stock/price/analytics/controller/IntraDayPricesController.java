package stock.price.analytics.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.client.finnhub.FinnhubClient;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
import stock.price.analytics.service.YahooQuoteService;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-prices")
public class IntraDayPricesController {

    private final YahooQuoteService yahooQuoteService;
    private final FinnhubClient finnhubClient;
    private final PriceOHLCRepository priceOHLCRepository;

    @GetMapping("/finnhub")
    public DailyPriceOHLC intraDayPrices(@RequestParam("ticker") String ticker) {
        return finnhubClient.intraDayPricesFor(ticker).orElseThrow();
    }

    @Transactional
    @GetMapping("/finnhub-all-xtb")
    public void finnhubIntraDayPricesTickersXTB() {
        List<DailyPriceOHLC> dailyPriceOHLCs = finnhubClient.intraDayPricesXTB();
        priceOHLCRepository.saveAll(dailyPriceOHLCs);
        log.info("saved {} daily prices", dailyPriceOHLCs.size());
    }

    @Transactional
    @GetMapping("/yahoo-prices")
    public void yahooPricesImport() {
        yahooQuoteService.dailyPricesImport();
    }

    @GetMapping("/yahoo-prices/from-file")
    public List<DailyPriceOHLC> yFinanceDailyPricesFrom(@RequestParam("fileName") String fileName) {
        return yahooQuoteService.dailyPricesFromFile(fileName);
    }

}