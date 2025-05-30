package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.highlow.HighLow4w;
import stock.price.analytics.model.prices.highlow.HighLow52Week;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.HighestLowestPrices;
import stock.price.analytics.model.prices.highlow.enums.HighLowPeriod;
import stock.price.analytics.model.prices.ohlc.DailyPrice;
import stock.price.analytics.repository.prices.highlow.HighLowForPeriodRepository;
import stock.price.analytics.util.QueryUtil;

import java.time.LocalDate;
import java.util.ArrayList;
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
    public List<? extends HighLowForPeriod> hlPricesForDate(HighLowPeriod highLowPeriod, LocalDate date) {
        Class<? extends HighLowForPeriod> hlClass = switch (highLowPeriod) {
            case HIGH_LOW_4W -> HighLow4w.class;
            case HIGH_LOW_52W -> HighLow52Week.class;
            case HIGH_LOW_ALL_TIME -> HighestLowestPrices.class;
        };
        String query = QueryUtil.highLowPricesNotDelistedForDateQuery(highLowPeriod);
        Query nativeQuery = entityManager.createNativeQuery(query, hlClass);
        nativeQuery.setParameter("tradingDate", date);
        return (List<? extends HighLowForPeriod>) nativeQuery.getResultList();
    }

    public void logNewHighLowsThisWeek() {
        List<String> newHighLowResult = new ArrayList<>();
        for (Object[] newHighLowThisWeek : highLowForPeriodRepository.newHighLowsThisWeek()) {
            boolean isFirst = true;
            for (Object newHL_col : newHighLowThisWeek) {
                if (isFirst) {
                    newHighLowResult.add("\"" + newHL_col + "\"");
                    isFirst = false;
                } else {
                    newHighLowResult.add(String.valueOf(newHL_col));
                }
            }
        }
        String newHighLowResultFormatted = String.join(" ", newHighLowResult);

        log.warn("-- {}", newHighLowResultFormatted);
    }
}
