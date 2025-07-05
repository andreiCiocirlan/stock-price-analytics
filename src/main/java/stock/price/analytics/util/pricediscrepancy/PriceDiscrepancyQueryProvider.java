package stock.price.analytics.util.pricediscrepancy;

import stock.price.analytics.model.prices.enums.StockTimeframe;

public interface PriceDiscrepancyQueryProvider {
    String updateStocksWithOpeningPriceDiscrepancyFor(StockTimeframe timeframe);
}
