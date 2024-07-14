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
import stock.price.analytics.util.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-prices")
public class IntraDayPricesController {

    private final YahooQuoteService yahooQuoteService;
    private final FinnhubClient finnhubClient;
    private final PriceOHLCRepository priceOHLCRepository;

    @GetMapping("/")
    public DailyPriceOHLC intraDayPrices(@RequestParam("ticker") String ticker) {
        return finnhubClient.intraDayPricesFor(ticker).orElseThrow();
    }

    @Transactional
    @GetMapping("/all-xtb")
    public void intraDayPricesAllTickersXTB() throws IOException, InterruptedException {
        List<String> tickers = FileUtils.readTickersXTB();
        List<DailyPriceOHLC> dailyPriceOHLCs = new ArrayList<>();

        for (String ticker : tickers) {
            Optional<DailyPriceOHLC> intraDayPrice = finnhubClient.intraDayPricesFor(ticker);
            intraDayPrice.ifPresent(dailyPrices -> addToListAndLog(dailyPrices, dailyPriceOHLCs));
            Thread.sleep(1005); // rate limit 60 req / min
        }

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

    private void addToListAndLog(DailyPriceOHLC dailyPrices, List<DailyPriceOHLC> dailyPriceOHLCs) {
        log.info("{}", dailyPrices);
        dailyPriceOHLCs.add(dailyPrices);
    }

}
