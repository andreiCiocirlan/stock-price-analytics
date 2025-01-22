package stock.price.analytics.cache;

import lombok.Getter;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.json.DailyPricesJSON;

import java.util.ArrayList;
import java.util.List;

@Getter
@Component
public class DailyPricesJSONCache {

    private final List<DailyPricesJSON> dailyPricesJSONCache = new ArrayList<>();

    public void addDailyJSONPrices(List<DailyPricesJSON> dailyPricesJSON) {
        dailyPricesJSONCache.addAll(dailyPricesJSON);
    }

}