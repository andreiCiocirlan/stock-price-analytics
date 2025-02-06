package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import stock.price.analytics.model.prices.ohlc.MonthlyPrice;
import stock.price.analytics.model.prices.ohlc.QuarterlyPrice;

import java.util.List;

public interface QuarterlyPricesRepository extends JpaRepository<QuarterlyPrice, Long> {

    @Query(value = """
                SELECT *
                FROM monthly_prices
                ORDER BY start_date desc
            """, nativeQuery = true)
    List<MonthlyPrice> findAllMonthlyPrices();

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
