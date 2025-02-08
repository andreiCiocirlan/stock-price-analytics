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

import static stock.price.analytics.util.EnumParser.isNoneEnum;
import static stock.price.analytics.util.EnumParser.parseEnumWithNoneValue;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final PriceMilestoneCache priceMilestoneCache;
    private final PreMarketPriceMilestoneCache preMarketPriceMilestoneCache;

    public List<String> findTickersForMilestone(String priceMilestone, List<Double> cfdMargins) {
        List<String> tickers = new ArrayList<>();
        Optional<PricePerformanceMilestone> priceMilestoneEnum = parseEnumWithNoneValue(priceMilestone, PricePerformanceMilestone.class);
        Optional<PreMarketPriceMilestone> preMarketMilestoneEnum = parseEnumWithNoneValue(priceMilestone, PreMarketPriceMilestone.class);

        if (priceMilestoneEnum.isPresent())
            tickers = priceMilestoneCache.findTickersForMilestone(priceMilestoneEnum.get(), cfdMargins);
        else if (preMarketMilestoneEnum.isPresent())
            tickers = preMarketPriceMilestoneCache.findTickersForPreMarketMilestone(preMarketMilestoneEnum.get(), cfdMargins);
        return tickers;
    }

    public boolean isNoneMilestone(String milestone) {
        return isNoneEnum(milestone, PricePerformanceMilestone.class) && isNoneEnum(milestone, PreMarketPriceMilestone.class);
    }

}
