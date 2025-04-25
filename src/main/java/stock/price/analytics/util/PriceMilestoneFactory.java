package stock.price.analytics.util;

import stock.price.analytics.model.prices.PriceMilestone;
import stock.price.analytics.model.prices.enums.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static stock.price.analytics.model.prices.enums.PreMarketGap.*;
import static stock.price.analytics.model.prices.enums.PreMarketPriceMilestone.*;

public class PriceMilestoneFactory {

    private static final Map<String, PriceMilestone> PRICE_MILESTONE_REGISTRY = createRegistry();

    private static Map<String, PriceMilestone> createRegistry() {
        return Stream.of(
                        PricePerformanceMilestone.values(),
                        NewHighLowMilestone.values(),
                        IntradayPriceSpike.values(),
                        PreMarketPriceMilestone.values(),
                        PreMarketGap.values(),
                        SimpleMovingAverageMilestone.values()
                )
                .flatMap(Arrays::stream)
                .collect(Collectors.toMap(
                        Enum::name,
                        Function.identity()
                ));
    }

    public static List<PriceMilestone> priceMilestonesFrom(List<String> milestoneCodes) {
        return milestoneCodes.stream()
                .map(PriceMilestoneFactory::priceMilestoneFrom)
                .toList();
    }

    public static PriceMilestone priceMilestoneFrom(String milestoneCode) {
        PriceMilestone milestone = PRICE_MILESTONE_REGISTRY.get(milestoneCode);
        if (milestone == null) {
            throw new IllegalArgumentException("Invalid milestone milestoneCode: " + milestoneCode);
        }
        return milestone;
    }

    public static List<PriceMilestone> preMarketSchedulerValues() {
        return List.of(PRE_NEW_52W_HIGH, PRE_NEW_52W_LOW, PRE_NEW_ALL_TIME_LOW, PRE_NEW_ALL_TIME_HIGH, GAP_DOWN_5_PERCENT, GAP_DOWN_10_PERCENT, GAP_UP_5_PERCENT, GAP_UP_10_PERCENT);
    }

    public static List<PriceMilestone> registry() {
        return new ArrayList<>(PRICE_MILESTONE_REGISTRY.values());
    }
}