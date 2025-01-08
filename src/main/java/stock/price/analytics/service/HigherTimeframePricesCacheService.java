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

    public void populateHigherTimeframePricesCache() {
        List<Stock> xtbStocks = stockRepository.findByXtbStockTrueAndDelistedDateIsNull();
        higherTimeframePricesCache.addWeeklyPrices(priceOHLCRepository.findPreviousThreeWeeklyPricesForTickers(xtbStocks.stream().map(Stock::getTicker).toList()));
        higherTimeframePricesCache.addMonthlyPrices(priceOHLCRepository.findPreviousThreeMonthlyPricesForTickers(xtbStocks.stream().map(Stock::getTicker).toList()));
        higherTimeframePricesCache.addYearlyPrices(priceOHLCRepository.findPreviousThreeYearlyPricesForTickers(xtbStocks.stream().map(Stock::getTicker).toList()));
    }

}
