package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HigherTimeframePricesCache;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.PriceOHLCRepository;
import stock.price.analytics.repository.stocks.StockRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HigherTimeframePricesCacheService {

    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final StockRepository stockRepository;
    private final PriceOHLCRepository priceOHLCRepository;

    public void initHigherTimeframePricesCache() {
        List<Stock> xtbStocks = stockRepository.findByXtbStockTrueAndDelistedDateIsNull();
        List<String> tickers = xtbStocks.stream().map(Stock::getTicker).toList();
        higherTimeframePricesCache.addWeeklyPrices(priceOHLCRepository.findPreviousThreeWeeklyPricesForTickers(tickers));
        higherTimeframePricesCache.addMonthlyPrices(priceOHLCRepository.findPreviousThreeMonthlyPricesForTickers(tickers));
        higherTimeframePricesCache.addYearlyPrices(priceOHLCRepository.findPreviousThreeYearlyPricesForTickers(tickers));
    }

}
