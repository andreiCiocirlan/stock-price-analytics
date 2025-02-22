package stock.price.analytics.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.model.prices.ohlc.*;
import stock.price.analytics.model.stocks.Stock;
import stock.price.analytics.repository.prices.MonthlyPricesRepository;
import stock.price.analytics.repository.prices.QuarterlyPricesRepository;
import stock.price.analytics.repository.prices.WeeklyPricesRepository;
import stock.price.analytics.repository.prices.YearlyPricesRepository;
import stock.price.analytics.repository.stocks.StockRepository;

import java.util.List;
import java.util.Map;

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

    public Map<String, ? extends AbstractPrice> getPricesByTickerAndDateFor(StockTimeframe timeframe) {
        return switch (timeframe) {
            case DAILY -> throw new IllegalStateException("Unexpected value DAILY");
            case WEEKLY -> higherTimeframePricesCache.getWeeklyPricesByTickerAndDate();
            case MONTHLY -> higherTimeframePricesCache.getMonthlyPricesByTickerAndDate();
            case QUARTERLY -> higherTimeframePricesCache.getQuarterlyPricesByTickerAndDate();
            case YEARLY -> higherTimeframePricesCache.getYearlyPricesByTickerAndDate();
        };
    }

    public void addPrices(List<? extends AbstractPrice> prices) {
        higherTimeframePricesCache.addPrices(prices);
    }

    public List<? extends AbstractPrice> pricesFor(List<String> tickers, StockTimeframe timeframe) {
        return higherTimeframePricesCache.pricesFor(tickers, timeframe);
    }
}
