package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.cache.model.PriceWithPrevClose;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

import java.util.ArrayList;
import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;
import static stock.price.analytics.model.stocks.enums.MarketState.REGULAR;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cache")
public class CacheController {

    private final CacheService cacheService;

    @GetMapping("/stocks")
    @ResponseStatus(HttpStatus.OK)
    public List<Stock> getCachedStocks() {
        return cacheService.getCachedStocks();
    }

    @GetMapping("/tickers")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getCachedTickers() {
        return cacheService.getCachedTickers();
    }

    @GetMapping("/pre-market-prices")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> getPreMarketDailyPricesCache() {
        return cacheService.getCachedDailyPrices(PRE);
    }

    @GetMapping("/intraday-prices")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> getIntradayDailyPricesCache() {
        return cacheService.getCachedDailyPrices(REGULAR);
    }

    @GetMapping("/htf-prices")
    public List<AbstractPrice> htfPricesFor(@RequestParam(required = false, value = "ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return cacheService.htfPricesFor(StockTimeframe.valueOf(timeFrame)).stream()
                .filter(p -> ticker == null || p.getTicker().equals(ticker))
                .toList();
    }

    @GetMapping("/htf-prices-with-prev-close")
    public List<PriceWithPrevClose> htfPricesWithPrevCloseFor(@RequestParam(required = false, value = "ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return cacheService.htfPricesWithPrevCloseFor(List.of(ticker), StockTimeframe.valueOf(timeFrame));
    }

    @GetMapping("/new-high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<String> newDailyHighLowsForHLPeriods(@RequestParam("highLowPeriod") HighLowPeriod highLowPeriod) {
        return cacheService.getNewHighLowsForHLPeriod(highLowPeriod);
    }

    @GetMapping("/equal-high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<String> equalHighLowsForHLPeriods(@RequestParam("highLowPeriod") HighLowPeriod highLowPeriod) {
        return cacheService.getEqualHighLowsForHLPeriod(highLowPeriod);
    }

    @GetMapping("/high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<? extends HighLowForPeriod> highLowsForTicker(@RequestParam("ticker") String ticker) {
        List<HighLowForPeriod> result = new ArrayList<>();
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            result.add(cacheService.highLowForPeriodPricesFor(highLowPeriod).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        }
        return result;
    }

    @GetMapping("/prev-week-high-lows")
    @ResponseStatus(HttpStatus.OK)
    public List<? extends HighLowForPeriod> prevWeekHighLowsForTicker(@RequestParam("ticker") String ticker) {
        List<HighLowForPeriod> result = new ArrayList<>();
        for (HighLowPeriod highLowPeriod : HighLowPeriod.values()) {
            result.add(cacheService.prevWeekHighLowForPeriodPricesFor(highLowPeriod).stream().filter(hlp -> ticker.equals(hlp.getTicker())).findFirst().orElseThrow());
        }
        return result;
    }

}
