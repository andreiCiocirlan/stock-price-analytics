package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.PreMarketPriceMilestoneCache;
import stock.price.analytics.cache.PriceMilestoneCache;
import stock.price.analytics.model.prices.enums.PreMarketPriceMilestone;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;

import java.util.*;

import static stock.price.analytics.util.EnumParser.isNoneEnum;
import static stock.price.analytics.util.EnumParser.parseEnumWithNoneValue;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceMilestoneService {

    private final PriceMilestoneCache priceMilestoneCache;
    private final PreMarketPriceMilestoneCache preMarketPriceMilestoneCache;

    public Map<String, List<String>> findTickersForMilestones(List<Enum<? extends Enum<?>>> priceMilestones, List<Double> cfdMargins) {
        Map<String, List<String>> tickersByPriceMilestones = new HashMap<>();
        for (Enum<? extends Enum<?>> priceMilestone : priceMilestones) {
            tickersByPriceMilestones.put(priceMilestone.toString(), findTickersForMilestone(priceMilestone.name(), cfdMargins));
        }
        return tickersByPriceMilestones;
    }

    public List<String> findTickersForMilestone(String priceMilestone, List<Double> cfdMargins) {
        final List<String> tickers = new ArrayList<>();
        Optional<PricePerformanceMilestone> pricePerformanceMilestone = parseEnumWithNoneValue(priceMilestone, PricePerformanceMilestone.class);
        if (pricePerformanceMilestone.isPresent()) {
            tickers.addAll(priceMilestoneCache.findTickersForMilestone(pricePerformanceMilestone.get(), cfdMargins));
        } else {
            parseEnumWithNoneValue(priceMilestone, PreMarketPriceMilestone.class)
                    .ifPresent(milestone -> tickers.addAll(preMarketPriceMilestoneCache.findTickersForPreMarketMilestone(milestone, cfdMargins)));
        }
        return tickers;
    }

    public boolean isNoneMilestone(String milestone) {
        return isNoneEnum(milestone, PricePerformanceMilestone.class) && isNoneEnum(milestone, PreMarketPriceMilestone.class);
    }

}
