package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface PriceOHLCRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    List<DailyPriceOHLC> findByTickerAndDateLessThan(String ticker, LocalDate date);

    List<DailyPriceOHLC> findByTickerAndDate(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM monthly_prices
                ORDER BY start_date desc
            """, nativeQuery = true)
    List<MonthlyPriceOHLC> findAllMonthlyPrices();

    @Query(value = """
                SELECT *
                FROM weekly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '2 week') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<WeeklyPriceOHLC> findPreviousThreeWeeklyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT w FROM WeeklyPriceOHLC w WHERE w.ticker = :ticker AND w.startDate < :date")
    List<WeeklyPriceOHLC> findWeeklyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT w FROM WeeklyPriceOHLC w WHERE w.ticker = :ticker AND w.startDate = :date")
    List<WeeklyPriceOHLC> findWeeklyByTickerAndStartDate(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM monthly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 month') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<MonthlyPriceOHLC> findPreviousThreeMonthlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT m FROM MonthlyPriceOHLC m WHERE m.ticker = :ticker AND m.startDate < :date")
    List<MonthlyPriceOHLC> findMonthlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT m FROM MonthlyPriceOHLC m WHERE m.ticker = :ticker AND m.startDate = :date")
    List<MonthlyPriceOHLC> findMonthlyByTickerAndStartDate(String ticker, LocalDate date);

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

    @Query(value = """
                SELECT *
                FROM yearly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 year') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<YearlyPriceOHLC> findPreviousThreeYearlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT y FROM YearlyPriceOHLC y WHERE y.ticker = :ticker AND y.startDate < :date")
    List<YearlyPriceOHLC> findYearlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT y FROM YearlyPriceOHLC y WHERE y.ticker = :ticker AND y.startDate = :date")
    List<YearlyPriceOHLC> findYearlyByTickerAndStartDate(String ticker, LocalDate date);
}