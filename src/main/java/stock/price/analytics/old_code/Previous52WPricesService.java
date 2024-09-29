package stock.price.analytics.old_code;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.ohlc.WeeklyPriceOHLC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Previous52WPricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final Previous52WPricesRepository previous52WeekPricesRepository;

    /**
        old code being called:

        List<WeeklyPriceOHLC> weeklyPricesUpdated = priceOHLCService.updatePricesForHigherTimeframes(dailyImportedPrices);

        Map<String, List<Previous52WPrices>> previous52wByTicker = previous52WPricesService.updatePrevious52wPrices(weeklyPricesUpdated);
        Map<HighLowPeriod, List<String>> stockHighLowUpdateMap = stockService.updateStocksHighLow(previous52wByTicker);

        refreshMaterializedViewsService.refreshMaterializedViews(); // refresh mviews first (high/low price update based on mview)
        highLowForPeriodService.saveCurrentWeekHighLowPrices(stockHighLowUpdateMap, tradingDateImported(dailyImportedPrices));
    */

    public Map<String, List<Previous52WPrices>> updatePrevious52wPrices(List<WeeklyPriceOHLC> weeklyPricesImported) {
        List<String> tickers = weeklyPricesImported.stream().map(WeeklyPriceOHLC::getTicker).toList();
        List<Previous52WPrices> prev52wUpdatedList = new ArrayList<>();

        Map<String, List<Previous52WPrices>> prev52weeksByTicker = previous52WeeksOrderedByStartDateDescForTickers(tickers).stream()
                .collect(Collectors.groupingBy(Previous52WPrices::getTicker));
        Map<String, WeeklyPriceOHLC> weeklyPricesImportedByTicker = weeklyPricesImported.stream()
                .collect(Collectors.toMap(WeeklyPriceOHLC::getTicker, p -> p));

        for (String ticker : weeklyPricesImportedByTicker.keySet()) {
            WeeklyPriceOHLC importedWeeklyPrice = weeklyPricesImportedByTicker.get(ticker);
            List<Previous52WPrices> previousEntries = prev52weeksByTicker.get(ticker);

            Previous52WPrices newEntry = Previous52WPrices.newFrom(importedWeeklyPrice);
            if (previousEntries != null && !previousEntries.isEmpty()) {
                Previous52WPrices latestPreviousEntry = previousEntries.getFirst();

                // if startDate matches -> update
                if (latestPreviousEntry.getStartDate().isEqual(importedWeeklyPrice.getStartDate())) {
                    latestPreviousEntry.updateFrom(importedWeeklyPrice);
                    prev52wUpdatedList.add(latestPreviousEntry);
                } else { // new prev52w
                    // Check if we need to remove the oldest entry
                    if (previousEntries.size() >= 52) { // remove 53rd week
                        Previous52WPrices oldestEntry = previousEntries.removeLast(); // ordered by start_date desc
                        previous52WeekPricesRepository.delete(oldestEntry);
                    }
                    prev52wUpdatedList.add(newEntry);
                }
            } else { // new ticker (first week IPO)
                prev52wUpdatedList.add(newEntry);
            }
        }
        previous52WeekPricesRepository.saveAll(prev52wUpdatedList);

        return prev52weeksByTicker;
    }

    private List<Previous52WPrices> previous52WeeksOrderedByStartDateDescForTickers(List<String> tickers) {
        return previous52WeekPricesRepository.prev52WeeksOrderedByStartDateDescForTickers(tickers);
    }

    /**
     * Run once after weekly_prices populated (afterward update with incoming data)
     */
    public void insertPrev52w() {
        String query = queryInsertPrev52w();
        int insertCount = entityManager.createNativeQuery(query).executeUpdate();
        if (insertCount != 0) {
            log.warn("inserted {} rows for Prev52w", insertCount);
        }
    }

    private String queryInsertPrev52w() {
        return """
                INSERT INTO PREV_52W
                SELECT nextval('sequence_prices') AS id, close, end_date, high, low, open, performance, start_date, ticker  FROM
                (
                	SELECT  close, end_date, high, low, open, performance, start_date, ticker
                	FROM (
                	    SELECT ticker, start_date, end_date, high, low, open, close, performance,
                	        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS rn
                	    FROM weekly_prices
                	) AS subquery
                	WHERE rn <= 52
                	ORDER BY ticker, start_date DESC
                )
                """;
    }

}
