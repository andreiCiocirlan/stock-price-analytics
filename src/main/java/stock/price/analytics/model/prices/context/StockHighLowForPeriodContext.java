package stock.price.analytics.model.prices.context;

import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.stocks.Stock;

public record StockHighLowForPeriodContext(Stock stock, HighLowForPeriod highLowForPeriod) {
}
