package stock.price.analytics.cache;

import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.PriceMilestone;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
class PriceMilestoneCache {

    private final Map<PriceMilestone, List<String>> tickersByPriceMilestones = new HashMap<>();

    public void cachePriceMilestoneTickers(PriceMilestone priceMilestone, List<String> tickersForMilestone) {
        tickersByPriceMilestones.put(priceMilestone, tickersForMilestone);
    }

    List<String> tickersFor(PriceMilestone priceMilestone) {
        return tickersByPriceMilestones.getOrDefault(priceMilestone, Collections.emptyList());
    }

    public void clearTickersByPriceMilestone(PriceMilestone milestone) {
        tickersByPriceMilestones.remove(milestone);
    }

}
