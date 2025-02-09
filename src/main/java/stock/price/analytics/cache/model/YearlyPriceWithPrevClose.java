package stock.price.analytics.cache.model;

import stock.price.analytics.model.prices.ohlc.YearlyPrice;

public record YearlyPriceWithPrevClose(YearlyPrice yearlyPrice, double previousClose) {

}