package stock.price.analytics.model.prices.highlow;

// query projection for updating high-low prices for different periods (4w, 52w, all-time)
public interface TickerHighLowView {

    String getTicker();

    Double getLow();

    Double getHigh();

}