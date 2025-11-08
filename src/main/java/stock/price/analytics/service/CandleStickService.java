package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.candle.CandleRangeQueryProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CandleStickService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final CandleRangeQueryProvider candleRangeQueryProvider;

    @SuppressWarnings("unchecked")
    public List<String> compressedPriceFor(StockTimeframe timeframe, String cfdMargins) {
        List<String> tickersCompressedPrice = new ArrayList<>();
        String query = candleRangeQueryProvider.compressedPriceQuery(timeframe, cfdMargins);

        List<Object[]> resultList = entityManager.createNativeQuery(query).getResultList();
        resultList.forEach(row -> tickersCompressedPrice.add((String) row[0]));

        return tickersCompressedPrice;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Double> averageCandleRangesFor(StockTimeframe timeframe) {
        Map<String, Double> avgCandleRanges = new HashMap<>();
        String query = candleRangeQueryProvider.averageCandleRangeQuery(timeframe);

        List<Object[]> resultList = entityManager.createNativeQuery(query).getResultList();
        resultList.forEach(row -> avgCandleRanges.put((String) row[0], (Double) row[1]));

        return avgCandleRanges;
    }
}
