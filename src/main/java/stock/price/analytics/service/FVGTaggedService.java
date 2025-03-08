package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.fvg.FairValueGap;
import stock.price.analytics.model.fvg.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FVGTaggedService {

    @PersistenceContext
    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Set<String> findTickersFVGsTaggedFor(StockTimeframe timeframe, FvgType fvgType, PricePerformanceMilestone pricePerformanceMilestone, String cfdMargins) {
        final String prefix = timeframe.stockPrefix();
        Pair<String, String> queryFields = switch (pricePerformanceMilestone) {
            case HIGH_52W_95, LOW_52W_95 -> new MutablePair<>("low52w", "high52w");
            case HIGH_4W_95, LOW_4W_95 -> new MutablePair<>("low4w", "high4w");
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> new MutablePair<>("lowest", "highest");
            case NEW_ALL_TIME_HIGH, NEW_52W_HIGH, NEW_4W_HIGH, NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW, NONE ->
                    throw new IllegalStateException("Unexpected value " + pricePerformanceMilestone.name());
        };
        String lowField = queryFields.getLeft();
        String highField = queryFields.getRight();
        String fvgTypeStr = fvgType.name();

        String highLowWhereClause;
        if (PricePerformanceMilestone.high95thPercentileValues().contains(pricePerformanceMilestone)) {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (1 - ((s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})))) > 0.95";
        } else {
            highLowWhereClause = STR."AND s.\{lowField} <> s.\{highField} AND (1 - (s.close - s.\{lowField}) / (s.\{highField} - s.\{lowField})) > 0.95";
        }

        String query = STR."""
                SELECT fvg.*
                FROM stocks s
                JOIN fvg on fvg.ticker = s.ticker AND fvg.status = 'OPEN' AND fvg.timeframe = '\{timeframe}' and fvg.type = '\{fvgTypeStr}'
                WHERE
                s.cfd_margin in (\{cfdMargins})
                \{highLowWhereClause}
                AND (s.\{prefix}high between fvg.low AND fvg.high OR s.\{prefix}low between fvg.low AND fvg.high)
                """;

        return ((List<FairValueGap>) entityManager.createNativeQuery(query, FairValueGap.class).getResultList())
                .stream()
                .map(FairValueGap::getTicker)
                .collect(Collectors.toSet());
    }
}
