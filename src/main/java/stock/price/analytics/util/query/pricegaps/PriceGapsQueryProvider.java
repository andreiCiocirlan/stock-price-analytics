package stock.price.analytics.util.query.pricegaps;

import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;

public interface PriceGapsQueryProvider {
    String saveHistoricalPriceGapsQueryFor(List<String> tickers, StockTimeframe timeframe);

    String saveTodayPriceGapsQueryFor(StockTimeframe timeframe);
}
