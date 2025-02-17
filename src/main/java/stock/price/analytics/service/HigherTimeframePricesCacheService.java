package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.HigherTimeframePricesCache;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.MonthlyPricesRepository;
import stock.price.analytics.repository.prices.QuarterlyPricesRepository;
import stock.price.analytics.repository.prices.WeeklyPricesRepository;
import stock.price.analytics.repository.prices.YearlyPricesRepository;
import stock.price.analytics.repository.stocks.StockRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HigherTimeframePricesCacheService {

    private final HigherTimeframePricesCache higherTimeframePricesCache;
    private final StockRepository stockRepository;
    private final WeeklyPricesRepository weeklyPricesRepository;
    private final MonthlyPricesRepository monthlyPricesRepository;
    private final QuarterlyPricesRepository quarterlyPricesRepository;
    private final YearlyPricesRepository yearlyPricesRepository;

    public void initHigherTimeframePricesCache() {
        List<Stock> xtbStocks = stockRepository.findByXtbStockTrueAndDelistedDateIsNull();
        List<String> tickers = xtbStocks.stream().map(Stock::getTicker).toList();
        higherTimeframePricesCache.addPrices(weeklyPricesRepository.findPreviousThreeWeeklyPricesForTickers(tickers));
        higherTimeframePricesCache.addPrices(monthlyPricesRepository.findPreviousThreeMonthlyPricesForTickers(tickers));
        higherTimeframePricesCache.addPrices(quarterlyPricesRepository.findPreviousThreeQuarterlyPricesForTickers(tickers));
        higherTimeframePricesCache.addPrices(yearlyPricesRepository.findPreviousThreeYearlyPricesForTickers(tickers));
    }

}
