package stock.price.analytics.model.prices.ohlc;

public record PriceWithPrevClose(AbstractPrice price, double previousClose) {
}
