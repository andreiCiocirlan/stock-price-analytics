package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.util.query.demark.DemarkQueryProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemarkService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final DemarkQueryProvider demarkQueryProvider;

    @Transactional
    public void demarkForTimeframe(StockTimeframe timeframe) {
        String query = demarkQueryProvider.demarkForTimeframeQuery(timeframe);
        int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
        log.warn("Inserted/Updated {} {} Demark indicator rows", rowsAffected, timeframe);
    }

}
