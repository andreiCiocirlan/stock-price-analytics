package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.stocks.Stock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StocksCache {

    private final Map<String, Stock> stocksMap = new HashMap<>();

    public void addStocks(List<Stock> stocks) {
        stocks.forEach(s -> stocksMap.merge(s.getTicker(), s, (_, newValue) -> newValue));
    }

    public List<String> tickers() {
        return stocksMap.keySet().stream().toList();
    }

    public List<Stock> stocksFor(List<String> tickers) {
        return tickers.stream()
                .map(stocksMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
