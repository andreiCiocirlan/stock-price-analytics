package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.candle.CandleRangeQueryProvider;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandleStickService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final CandleRangeQueryProvider candleRangeQueryProvider;

    @SuppressWarnings("unchecked")
    public List<String> compressedPriceFor(StockTimeframe timeframe, String cfdMargins) {
        String query = candleRangeQueryProvider.compressedPriceQuery(timeframe, cfdMargins);

        return (List<String>) entityManager.createNativeQuery(query).getResultList();
    }

}
