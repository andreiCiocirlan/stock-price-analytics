package stock.price.analytics.util.query.candle;

import stock.price.analytics.model.prices.enums.StockTimeframe;

public interface CandleRangeQueryProvider {
    String averageCandleRangeQuery(StockTimeframe timeframe);
}
