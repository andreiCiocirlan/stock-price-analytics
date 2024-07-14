package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.controller.dto.CandleOHLCWithDateDTO;
import stock.price.analytics.model.prices.enums.StockTimeframe;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceOHLCService {

    @PersistenceContext
    private final EntityManager entityManager;

    public List<CandleOHLCWithDateDTO> findOHLCFor(String ticker, StockTimeframe timeframe) {
        String tableNameOHLC = StockTimeframe.dbTableOHLCFrom(timeframe);
        String orderByIdField = timeframe == StockTimeframe.DAILY ? "date" : "start_date";
        String queryStr = STR."SELECT \{orderByIdField}, open, high, low, close FROM \{tableNameOHLC} WHERE ticker = :ticker ORDER BY \{orderByIdField} ASC";

        Query nativeQuery = entityManager.createNativeQuery(queryStr, CandleOHLCWithDateDTO.class);
        nativeQuery.setParameter("ticker", ticker);

        @SuppressWarnings("unchecked")
        List<CandleOHLCWithDateDTO> priceOHLCs = (List<CandleOHLCWithDateDTO>) nativeQuery.getResultList();

        return priceOHLCs;
    }

    @Transactional
    public void updateHigherTimeframesPricesFor(String dateFormatted) {
        updateHigherTimeframeHistPrices("week", "weekly_prices", dateFormatted);
        updateHigherTimeframeHistPrices("month", "monthly_prices", dateFormatted);
        updateHigherTimeframeHistPrices("year", "yearly_prices", dateFormatted);
    }

    public void saveHigherTimeframePricesBetween(LocalDate startDate, LocalDate endDate) {
        while (startDate.isBefore(endDate)) {
            String dateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            updateHigherTimeframesPricesFor(dateStr);
            startDate = startDate.plusDays(7);
        }
    }

    private void updateHigherTimeframeHistPrices(String timeframe, String tableName, String dateFormatted) {
        String query = STR."""
            WITH interval_data AS (
            SELECT
                ticker,
                MIN(date) AS start_date,
                MAX(date) AS end_date,
                MAX(high) AS high,
                MIN(low) AS low
            FROM daily_prices
            WHERE date BETWEEN '\{dateFormatted}'::date - INTERVAL '2 \{timeframe}' AND '\{dateFormatted}'
            GROUP BY ticker, DATE_TRUNC('\{timeframe}', date)
            ),
            last_week AS (
                SELECT
                    dp_o.ticker,
                    start_date,
                    end_date,
                    interval_data.high,
                    interval_data.low,
                    dp_o.open,
                    dp_c.close,
                    CASE
                        WHEN (dp_o.date >= DATE_TRUNC('\{timeframe}', CURRENT_DATE)) THEN
                                    ROUND((100.0 * (dp_c.close - dp_o.open) / dp_o.open)::numeric, 2)
                        WHEN (LAG(dp_c.close, 1) OVER (PARTITION BY dp_o.ticker ORDER BY start_date) IS NULL) THEN
                            CASE
                                WHEN (dp_o.open <> 0) THEN ROUND((100.0 * (dp_c.close - dp_o.open) / dp_o.open)::numeric, 2)
                                ELSE NULL
                            END
                        ELSE
                            ROUND(((dp_c.close - LAG(dp_c.close) OVER (PARTITION BY dp_o.ticker ORDER BY start_date)) / LAG(dp_c.close) OVER (PARTITION BY dp_o.ticker ORDER BY start_date) * 100)::numeric, 2)
                    END AS performance
                FROM interval_data
                JOIN daily_prices dp_o ON dp_o.ticker = interval_data.ticker AND dp_o.date = interval_data.start_date
                JOIN daily_prices dp_c ON dp_c.ticker = interval_data.ticker AND dp_c.date = interval_data.end_date
            ),
            final_result AS (
                SELECT *
                FROM last_week
                    WHERE end_date BETWEEN DATE_TRUNC('week', '\{dateFormatted}'::date) AND '\{dateFormatted}' -- 'week' to capture EOY/EOM where 31st on Mon/Tue/Wed/Thu
            )
            INSERT INTO \{tableName} (ticker, start_date, end_date, high, low, open, close, performance)
            SELECT ticker, start_date, end_date, high, low, open, close, performance
            FROM final_result
            ON CONFLICT (ticker, start_date)
                DO UPDATE SET
                    high = EXCLUDED.high,
                    low = EXCLUDED.low,
                    close = EXCLUDED.close,
                    performance = EXCLUDED.performance,
                    end_date = EXCLUDED.end_date
            """;

        int savedOrUpdatedCount = entityManager.createNativeQuery(
                String.format(query, timeframe, tableName)
        ).executeUpdate();
        if (savedOrUpdatedCount != 0) {
            log.info("saved/updated {} {} rows for date {}", savedOrUpdatedCount, timeframe, dateFormatted);
        }
    }
}
