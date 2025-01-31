package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.PriceMilestoneCache;
import stock.price.analytics.model.prices.enums.PriceMilestone;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final PriceMilestoneCache priceMilestoneCache;

    public List<String> findTickersForMilestone(PriceMilestone priceMilestone, double cfdMargin) {
        return priceMilestoneCache.findTickersForMilestone(priceMilestone, cfdMargin);
    }

}
