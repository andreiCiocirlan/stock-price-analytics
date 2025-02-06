package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HigherTimeframePricesCache;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.MonthlyPricesRepository;
import stock.price.analytics.repository.prices.PricesRepository;
import stock.price.analytics.repository.prices.WeeklyPricesRepository;
import stock.price.analytics.repository.stocks.StockRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HigherTimeframePricesCacheService {

    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final StockRepository stockRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final PricesRepository pricesRepository;

    public void initHigherTimeframePricesCache() {
        List<Stock> xtbStocks = stockRepository.findByXtbStockTrueAndDelistedDateIsNull();
        List<String> tickers = xtbStocks.stream().map(Stock::getTicker).toList();
        higherTimeframePricesCache.addWeeklyPrices(weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers));
        higherTimeframePricesCache.addMonthlyPrices(monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers));
        higherTimeframePricesCache.addQuarterlyPrices(pricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers));
        higherTimeframePricesCache.addYearlyPrices(pricesRepository.findPreviousThreeYearlyPricesForTickers(tickers));
    }

}
