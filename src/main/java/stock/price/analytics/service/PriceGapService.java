package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.gaps.PriceGapRepository;
import stock.price.analytics.util.query.pricegaps.PriceGapsQueryProvider;

import java.util.List;

import static stock.price.analytics.model.prices.enums.StockTimeframe.WEEKLY;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceGapService {

    private final PriceGapRepository priceGapRepository;
    private final PriceService priceService;
    private final CacheService cacheService;

    @PersistenceContext
    private final EntityManager entityManager;
    private final PriceGapsQueryProvider priceGapsQueryProvider;

    @Transactional
    public void saveAllPriceGapsFor(List<String> tickers) {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            savePriceGapsFor(tickers, timeframe, true);
        }
    }

    @Transactional
    public void savePriceGapsTodayFor(List<String> tickers, StockTimeframe timeframe) {
        savePriceGapsFor(tickers, timeframe, false);
    }

    @Transactional
    public void savePriceGapsTodayForAllTickers() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            savePriceGapsFor(cacheService.getCachedTickers(), timeframe, false);
        }
    }

    @Transactional
    private void savePriceGapsFor(List<String> tickers, StockTimeframe timeframe, boolean allHistoricalData) {
        String query = priceGapsQueryProvider.savePriceGapsQueryFor(tickers, timeframe, allHistoricalData, priceService.isFirstImportDoneFor(WEEKLY));
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
