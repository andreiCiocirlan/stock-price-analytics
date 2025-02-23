package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.AbstractPrice;

// Marker interface
public interface PriceWithPrevClose {
    AbstractPrice getPrice();
}
