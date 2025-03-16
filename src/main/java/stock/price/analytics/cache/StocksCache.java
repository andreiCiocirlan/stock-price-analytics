package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.stocks.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Component
class StocksCache {

    private final Map<String, Stock> stocksMap = new HashMap<>();

    void addStocks(List<Stock> stocks) {
        stocks.forEach(s -> stocksMap.merge(s.getTicker(), s, (_, newValue) -> newValue));
    }

    List<String> getCachedTickers() {
        return new ArrayList<>(getStocksMap().values().stream().map(Stock::getTicker).toList());
    }

    List<Stock> getCachedStocks() {
        return new ArrayList<>(getStocksMap().values().stream().toList());
    }
}
