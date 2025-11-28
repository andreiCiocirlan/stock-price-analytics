package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.gaps.PriceGapRepository;
import stock.price.analytics.util.query.pricegaps.PriceGapsQueryProvider;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceGapService {

    private final PriceGapRepository priceGapRepository;

    @PersistenceContext
    private final EntityManager entityManager;
    private final PriceGapsQueryProvider priceGapsQueryProvider;

    @Transactional
    public void saveHistoricalPriceGapsFor(List<String> tickers) {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            saveHistoricalPriceGapsFor(tickers, timeframe);
        }
    }

    @Transactional
    public void savePriceGapsTodayFor(StockTimeframe timeframe) {
        String query = priceGapsQueryProvider.saveTodayPriceGapsQueryFor(timeframe);
        int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
        log.info("saved {} rows for {} price gaps", rowsAffected, timeframe);
    }

    @Transactional
    private void saveHistoricalPriceGapsFor(List<String> tickers, StockTimeframe timeframe) {
        String query = priceGapsQueryProvider.saveHistoricalPriceGapsQueryFor(tickers, timeframe);
        int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
        log.info("saved {} rows for {} price gaps", rowsAffected, timeframe);
    }

    @Transactional
    public void closePriceGaps() {
        int rowsAffected = priceGapRepository.closePriceGaps();
        if (rowsAffected != 0) {
            log.info("Closed {} price gaps", rowsAffected);
        }
    }
}
