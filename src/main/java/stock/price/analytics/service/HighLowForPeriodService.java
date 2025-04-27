package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;
import stock.price.analytics.util.QueryUtil;

import java.time.LocalDate;
import java.util.List;

import static stock.price.analytics.model.prices.highlow.enums.HighLowPeriod.values;
import static stock.price.analytics.util.LoggingUtil.logTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HighLowForPeriodService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final HighLowForPeriodRepository highLowForPeriodRepository;
    private final CacheService cacheService;
    private final AsyncPersistenceService asyncPersistenceService;

    @Transactional
    public void saveCurrentWeekHighLowPricesFrom(List<DailyPrice> dailyPrices) {
        List<String> tickers = dailyPrices.stream().map(DailyPrice::getTicker).toList();
        for (HighLowPeriod highLowPeriod : values()) {
            List<? extends HighLowForPeriod> hlPricesUpdated = cacheService.getUpdatedHighLowPricesForTickers(dailyPrices, tickers, highLowPeriod);
            if (!hlPricesUpdated.isEmpty()) {
                asyncPersistenceService.partitionDataAndSaveNoLogging(hlPricesUpdated, highLowForPeriodRepository);
                cacheService.addHighLowPrices(hlPricesUpdated, highLowPeriod);
            }
        }
    }

    @Transactional
    public void saveAllHistoricalHighLowPrices(List<String> tickers, LocalDate tradingDate) {
        logTime(() -> executeQueryAllHistoricalHLPrices(tickers, tradingDate), "saved ALL historical HighLow prices for " + tickers);
    }

    private void executeQueryAllHistoricalHLPrices(List<String> tickers, LocalDate tradingDate) {
        for (HighLowPeriod highLowPeriod : values()) {
            String query = QueryUtil.queryAllHistoricalHighLowPricesFor(tickers, tradingDate, highLowPeriod);
            int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
            log.info("saved {} rows for {} and high low period {}", rowsAffected, tickers, highLowPeriod);
        }
    }

    public boolean weeklyHighLowExists() {
        String query = QueryUtil.weeklyHighLowExistsQuery();
        Query nativeQuery = entityManager.createNativeQuery(query, Boolean.class);
        return (Boolean) nativeQuery.getResultList().getFirst();
    }

    @SuppressWarnings("unchecked")
    public List<HighLowForPeriod> highLowPricesNotDelistedForDate(HighLowPeriod highLowPeriod, LocalDate date) {
        String query = QueryUtil.highLowPricesNotDelistedForDateQuery(highLowPeriod, date);
        return ((List<HighLowForPeriod>) entityManager.createNativeQuery(query, HighLowForPeriod.class).getResultList());
    }
}
