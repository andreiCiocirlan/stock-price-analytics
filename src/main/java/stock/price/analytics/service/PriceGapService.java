package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.repository.gaps.PriceGapRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceGapService {

    private final PriceGapRepository priceGapRepository;
    private final PriceService priceService;

    @PersistenceContext
    private final EntityManager entityManager;

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
    private void savePriceGapsFor(List<String> tickers, StockTimeframe timeframe, boolean allHistoricalData) {
        String tickersFormatted = tickers.stream().map(ticker -> STR."'\{ticker}'").collect(Collectors.joining(", "));
        String dbTable = timeframe.dbTableOHLC();
        String dateColumn = timeframe == StockTimeframe.DAILY ? "date" : "start_date";
        String dateTruncPeriod = timeframe.toDateTruncPeriod();
        String interval = timeframe.toInterval();
        String intervalPeriod = timeframe.toIntervalPeriod();
        int lookBackCount = timeframe == StockTimeframe.DAILY && priceService.isFirstImportDoneFor(StockTimeframe.WEEKLY) ? 3 : 1;
        if (allHistoricalData) {
            lookBackCount = switch (timeframe) {
                case DAILY -> 1000;
                case WEEKLY -> 300;
                case MONTHLY -> 200;
                case QUARTERLY -> 100;
                case YEARLY -> 10;
            };
        }

        String query = STR."""
            WITH max_date_cte AS (
                select date_trunc('\{dateTruncPeriod}', (select max(last_updated) from stocks)) - INTERVAL '\{interval}' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    \{dateColumn} AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY \{dateColumn} DESC) AS row_num
                FROM \{dbTable}
                WHERE ticker in (\{tickersFormatted})
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33))
                    AND \{dateColumn} between CURRENT_DATE - INTERVAL '\{lookBackCount} \{intervalPeriod}' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= \{lookBackCount} and NOT EXISTS (
                    SELECT 1
                    FROM \{dbTable} p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.\{dateColumn} > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'\{timeframe}',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """;

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
