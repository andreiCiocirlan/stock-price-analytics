package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.gaps.FairValueGap;
import stock.price.analytics.model.gaps.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;

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
        String query = findTickersFVGsTaggedQueryFor(timeframe, fvgType, pricePerformanceMilestone, cfdMargins);
        return ((List<FairValueGap>) entityManager.createNativeQuery(query, FairValueGap.class).getResultList())
                .stream()
                .map(FairValueGap::getTicker)
                .collect(Collectors.toSet());
    }

    private static String findTickersFVGsTaggedQueryFor(StockTimeframe timeframe, FvgType fvgType, PricePerformanceMilestone pricePerformanceMilestone, String cfdMargins) {
        String prefix = timeframe.stockPrefix();
        Pair<String, String> queryFields = switch (pricePerformanceMilestone) {
            case HIGH_52W_95, LOW_52W_95, HIGH_52W_90, LOW_52W_90 -> new MutablePair<>("low52w", "high52w");
            case HIGH_4W_95, LOW_4W_95, HIGH_4W_90, LOW_4W_90 -> new MutablePair<>("low4w", "high4w");
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95, HIGH_ALL_TIME_90, LOW_ALL_TIME_90 -> new MutablePair<>("lowest", "highest");
        };
        String lowField = queryFields.getLeft();
        String highField = queryFields.getRight();
        String fvgTypeStr = fvgType.name();
        String highLowWhereClause = highLowWhereClauseFVGsTagged(pricePerformanceMilestone, lowField, highField);

        return STR."""
                SELECT fvg.*
                FROM stocks s
                JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = '\{timeframe}' and fvg.type = '\{fvgTypeStr}'
                WHERE
                s.cfd_margin in (\{cfdMargins})
                \{highLowWhereClause}
                AND (s.\{prefix}high between fvg.low AND fvg.high OR s.\{prefix}low between fvg.low AND fvg.high)
                """;
    }

    private static String highLowWhereClauseFVGsTagged(PricePerformanceMilestone pricePerformanceMilestone, String lowField, String highField) {
        String highLowWhereClause;
        double percentage = pricePerformanceMilestone.is95thPercentileValue() ? 0.95 : 0.9;
        if (PricePerformanceMilestone.highPercentileValues().contains(pricePerformanceMilestone)) {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (1 - ((s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})))) > \{percentage}";
        } else {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})) > \{percentage}";
        }
        return highLowWhereClause;
    }
}
