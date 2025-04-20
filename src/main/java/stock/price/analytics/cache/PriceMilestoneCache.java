package stock.price.analytics.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.enums.IntradayPriceSpike;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.INTRADAY_SPIKE_DOWN;
import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.INTRADAY_SPIKE_UP;

@Slf4j
@Component
class PriceMilestoneCache {

    private final Map<PriceMilestone, List<String>> tickersByPriceMilestones = new HashMap<>();

    public void cachePriceMilestoneTickers(PriceMilestone priceMilestone, List<String> tickersForMilestone) {
        tickersByPriceMilestones.put(priceMilestone, tickersForMilestone);
    }

    Map<PriceMilestone, List<String>> tickersByPriceMilestones() {
        return tickersByPriceMilestones;
    }

    List<String> tickersFor(PriceMilestone priceMilestone) {
        return tickersByPriceMilestones.getOrDefault(priceMilestone, Collections.emptyList());
    }

    public void clearTickersByPriceMilestone(PriceMilestone milestone) {
        tickersByPriceMilestones.remove(milestone);
    }

    void addIntradaySpike(IntradayPriceSpike intradayPriceSpike, String ticker) {
        switch (intradayPriceSpike) {
            case INTRADAY_SPIKE_UP -> {
                tickersByPriceMilestones.get(INTRADAY_SPIKE_UP).add(ticker);
                tickersByPriceMilestones.get(INTRADAY_SPIKE_DOWN).remove(ticker);
            }
            case INTRADAY_SPIKE_DOWN -> {
                tickersByPriceMilestones.get(INTRADAY_SPIKE_DOWN).add(ticker);
                tickersByPriceMilestones.get(INTRADAY_SPIKE_UP).remove(ticker);
            }
        }
    }

    public void clearIntradaySpikes() {
        tickersByPriceMilestones.get(INTRADAY_SPIKE_UP).clear();
        tickersByPriceMilestones.get(INTRADAY_SPIKE_DOWN).clear();
    }
}
