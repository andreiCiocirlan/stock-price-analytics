package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;

public record PriceWithPrevClose(AbstractPrice abstractPrice, double previousClose) {
}
