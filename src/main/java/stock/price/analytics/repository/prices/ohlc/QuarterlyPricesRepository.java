package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import stock.price.analytics.model.prices.ohlc.QuarterlyPrice;

import java.time.LocalDate;
import java.util.List;

public interface QuarterlyPricesRepository extends JpaRepository<QuarterlyPrice, Long> {

    List<QuarterlyPrice> findByTickerIn(List<String> tickers);

    @Query(value = """
                SELECT *
                FROM quarterly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('quarter', CURRENT_DATE) - INTERVAL '6 month') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<QuarterlyPrice> findPreviousThreeQuarterlyPricesForTickers(List<String> tickers);

    @Query("SELECT q FROM QuarterlyPrice q WHERE q.ticker = :ticker AND q.startDate < :date")
    List<QuarterlyPrice> findQuarterlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT q FROM QuarterlyPrice q WHERE q.ticker = :ticker AND q.startDate = :date")
    List<QuarterlyPrice> findQuarterlyByTickerAndStartDate(String ticker, LocalDate date);

    @Modifying
    @Query(value = """
            WITH quarters_prev_close AS (
            	SELECT
            		ticker,
            		start_date,
            		open as opening_price,
            		close as closing_price,
            		LAG(close) OVER (PARTITION BY ticker ORDER BY start_date) AS previous_close
            	FROM
            		quarterly_prices
            )
            UPDATE quarterly_prices
            SET performance =
            	CASE
            		WHEN previous_close IS NULL THEN ROUND((closing_price::numeric - opening_price::numeric) / opening_price::numeric * 100, 2)  -- No previous quarter
            		ELSE ROUND((closing_price::numeric - previous_close::numeric) / previous_close::numeric * 100, 2)
            	END
            FROM quarters_prev_close qpc
            WHERE quarterly_prices.ticker = qpc.ticker and quarterly_prices.start_date = qpc.start_date;
            """, nativeQuery = true)
    void quarterlyPricesUpdatePerformance();
}
