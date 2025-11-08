package stock.price.analytics.util.query.candle;

import stock.price.analytics.model.prices.enums.StockTimeframe;

public interface CandleRangeQueryProvider {
    String compressedPriceQuery(StockTimeframe timeframe, String cfdMargins);
    String averageCandleRangeQuery(StockTimeframe timeframe);
}
