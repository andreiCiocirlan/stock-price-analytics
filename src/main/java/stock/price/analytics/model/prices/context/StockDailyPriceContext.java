package stock.price.analytics.model.prices.context;

import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.model.stocks.Stock;

public record StockDailyPriceContext(Stock stock, DailyPrice dailyPrice) {}
