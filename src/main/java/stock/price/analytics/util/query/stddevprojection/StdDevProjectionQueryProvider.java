package stock.price.analytics.util.query.stddevprojection;

import stock.price.analytics.model.prices.enums.StockTimeframe;

public interface StdDevProjectionQueryProvider {
    String insertStdDevProjections(StockTimeframe timeframe);
    String getConfluenceProjections(String ticker, StockTimeframe timeframe);
}
