package stock.price.analytics.util.pricegaps;

import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;

public interface PriceGapsQueryProvider {
    String savePriceGapsQueryFor(List<String> tickers, StockTimeframe timeframe, boolean allHistoricalData, boolean firstWeeklyImportDone);
}
