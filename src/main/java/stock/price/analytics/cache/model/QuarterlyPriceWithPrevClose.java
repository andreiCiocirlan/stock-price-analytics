package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.QuarterlyPrice;

public record QuarterlyPriceWithPrevClose(QuarterlyPrice quarterlyPrice, double previousClose) {
}
