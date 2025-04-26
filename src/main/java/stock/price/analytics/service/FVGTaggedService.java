package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.gaps.FairValueGap;
import stock.price.analytics.model.gaps.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.QueryUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FVGTaggedService {

    @PersistenceContext
    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Set<String> findTickersFVGsTaggedFor(StockTimeframe timeframe, FvgType fvgType, PricePerformanceMilestone pricePerformanceMilestone, String cfdMargins) {
        String query = QueryUtil.findTickersFVGsTaggedQueryFor(timeframe, fvgType, pricePerformanceMilestone, cfdMargins);
        return ((List<FairValueGap>) entityManager.createNativeQuery(query, FairValueGap.class).getResultList())
                .stream()
                .map(FairValueGap::getTicker)
                .collect(Collectors.toSet());
    }

}
