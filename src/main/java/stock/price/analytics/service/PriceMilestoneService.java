package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.PreMarketPriceMilestoneCache;
import stock.price.analytics.cache.PriceMilestoneCache;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.enums.PriceMilestone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final PriceMilestoneCache priceMilestoneCache;
    private final PreMarketPriceMilestoneCache preMarketPriceMilestoneCache;

    public List<String> findTickersForMilestone(String priceMilestone, double cfdMargin) {
        List<String> tickers = new ArrayList<>();
        Optional<PriceMilestone> priceMilestoneEnum = getPriceMilestone(priceMilestone);
        Optional<PreMarketPriceMilestone> preMarketMilestoneEnum = getPreMarketPriceMilestone(priceMilestone);

        if (priceMilestoneEnum.isPresent())
            tickers = priceMilestoneCache.findTickersForMilestone(priceMilestoneEnum.get(), cfdMargin);
        else if (preMarketMilestoneEnum.isPresent())
            tickers = preMarketPriceMilestoneCache.findTickersForPreMarketMilestone(preMarketMilestoneEnum.get(), cfdMargin);
        return tickers;
    }

    private Optional<PriceMilestone> getPriceMilestone(String milestone) {
        try {
            return PriceMilestone.valueOf(milestone) == PriceMilestone.NONE ? Optional.empty() : Optional.of(PriceMilestone.valueOf(milestone));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // Not a valid PriceMilestone
        }
    }

    private Optional<PreMarketPriceMilestone> getPreMarketPriceMilestone(String milestone) {
        try {
            return PreMarketPriceMilestone.valueOf(milestone) == PreMarketPriceMilestone.NONE ? Optional.empty() : Optional.of(PreMarketPriceMilestone.valueOf(milestone));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // Not a valid PreMarketPriceMilestone
        }
    }

    public boolean isNoneMilestone(String milestone) {
        return getPriceMilestone(milestone).isEmpty() && getPreMarketPriceMilestone(milestone).isEmpty();
    }

}
