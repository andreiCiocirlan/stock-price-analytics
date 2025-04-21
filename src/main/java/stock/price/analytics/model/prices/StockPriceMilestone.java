package stock.price.analytics.model.prices;

@FunctionalInterface
public interface StockPriceMilestone<T> extends PriceMilestone {
    boolean isMetFor(T context);
}
