package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.json.DailyPriceJSON;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.prices.ohlc.PriceWithPrevClose;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.util.Constants;
import stock.price.analytics.util.PriceMilestoneFactory;

import java.util.ArrayList;
import java.util.List;

import static stock.price.analytics.model.stocks.enums.MarketState.PRE;


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

    @GetMapping("/daily-prices-json")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPriceJSON> getDailyPriceJsonCache(@RequestParam(required = false, value = "ticker") String ticker) {
        return cacheService.dailyPriceJsonCache().stream()
                .filter(p -> ticker == null || p.getSymbol().equals(ticker))
                .toList();
    }

    @GetMapping("/pre-market-prices")
    @ResponseStatus(HttpStatus.OK)
    public List<DailyPrice> getPreMarketDailyPricesCache(@RequestParam(required = false, value = "ticker") String ticker) {
        return cacheService.getCachedDailyPrices(PRE).stream()
                .filter(p -> ticker == null || p.getTicker().equals(ticker))
                .toList();
    }

    @GetMapping("/prices")
    public List<AbstractPrice> pricesFor(@RequestParam(required = false, value = "ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return cacheService.pricesFor(StockTimeframe.valueOf(timeFrame)).stream()
                .filter(p -> ticker == null || p.getTicker().equals(ticker))
                .toList();
    }

    @GetMapping("/prices-with-prev-close")
    public List<PriceWithPrevClose> pricesWithPrevCloseFor(@RequestParam(required = false, value = "ticker") String ticker, @RequestParam("timeFrame") String timeFrame) {
        return cacheService.pricesWithPrevCloseFor(List.of(ticker), StockTimeframe.valueOf(timeFrame));
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

    @GetMapping("/price-milestone")
    @ResponseStatus(HttpStatus.OK)
    public List<String> priceMilestoneTickersFor(@RequestParam("priceMilestone") String priceMilestoneStr) {
        try {
            return cacheService.tickersFor(PriceMilestoneFactory.priceMilestoneFrom(priceMilestoneStr), Constants.CFD_MARGINS_5X_4X_3X_2X);
        } catch (RuntimeException e) {
            return List.of("Invalid enum type and price milestone combo!");
        }
    }

}
