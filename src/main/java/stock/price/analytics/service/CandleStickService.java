package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.util.QueryUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CandleStickService {

    @PersistenceContext
    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Map<String, Double> averageCandleLength15Days() {
        Map<String, Double> avgCandleLength15DaysByTicker = new HashMap<>();
        String query = QueryUtil.averageCandleLength15DaysQuery();

        List<Object[]> resultList = entityManager.createNativeQuery(query).getResultList();
        resultList.forEach(row -> avgCandleLength15DaysByTicker.put((String) row[0], (Double) row[1]));

        return avgCandleLength15DaysByTicker;
    }
}
