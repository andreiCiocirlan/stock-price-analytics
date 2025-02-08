package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.PreMarketPriceMilestoneCache;
import stock.price.analytics.cache.PriceMilestoneCache;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final PriceMilestoneCache priceMilestoneCache;
    private final PreMarketPriceMilestoneCache preMarketPriceMilestoneCache;

    public List<String> findTickersForMilestone(String priceMilestone, List<Double> cfdMargins) {
        List<String> tickers = new ArrayList<>();
        Optional<PricePerformanceMilestone> priceMilestoneEnum = getPricePerformanceMilestone(priceMilestone);
        Optional<PreMarketPriceMilestone> preMarketMilestoneEnum = getPreMarketPriceMilestone(priceMilestone);

        if (priceMilestoneEnum.isPresent())
            tickers = priceMilestoneCache.findTickersForMilestone(priceMilestoneEnum.get(), cfdMargins);
        else if (preMarketMilestoneEnum.isPresent())
            tickers = preMarketPriceMilestoneCache.findTickersForPreMarketMilestone(preMarketMilestoneEnum.get(), cfdMargins);
        return tickers;
    }

    private Optional<PricePerformanceMilestone> getPricePerformanceMilestone(String milestone) {
        try {
            return PricePerformanceMilestone.valueOf(milestone) == PricePerformanceMilestone.NONE ? Optional.empty() : Optional.of(PricePerformanceMilestone.valueOf(milestone));
        } catch (IllegalArgumentException e) {
            log.error("Invalid PriceMilestone {} error: {}", milestone, e.getMessage());
            return Optional.empty(); // Not a valid PriceMilestone
        }
    }

    private Optional<PreMarketPriceMilestone> getPreMarketPriceMilestone(String milestone) {
        try {
            return PreMarketPriceMilestone.valueOf(milestone) == PreMarketPriceMilestone.NONE ? Optional.empty() : Optional.of(PreMarketPriceMilestone.valueOf(milestone));
        } catch (IllegalArgumentException e) {
            log.error("Invalid PreMarketPriceMilestone {} error: {}", milestone, e.getMessage());
            return Optional.empty(); // Not a valid PreMarketPriceMilestone
        }
    }

    public boolean isNoneMilestone(String milestone) {
        return milestone != null && getPricePerformanceMilestone(milestone).isEmpty() && getPreMarketPriceMilestone(milestone).isEmpty();
    }

}
