package stock.price.analytics.model.prices.ohlc;

public record PriceWithPrevClose(AbstractPrice abstractPrice, double previousClose) {
}
