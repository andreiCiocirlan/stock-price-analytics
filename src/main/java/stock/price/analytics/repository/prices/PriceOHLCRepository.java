package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.*;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface PriceOHLCRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    List<DailyPriceOHLC> findByTickerAndDateLessThanEqual(String ticker, LocalDate date);

    List<DailyPriceOHLC> findByTickerAndDate(String ticker, LocalDate date);

    @Query(value = """
            SELECT id, ticker, start_date, end_date, high, low, open, close, performance
            FROM (
                SELECT id, ticker, start_date, end_date, high, low, open, close, performance,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS rn
                FROM weekly_prices
                WHERE ticker IN (:tickers)
            ) AS subquery
            WHERE rn <= 2
            ORDER BY ticker, start_date DESC;
            """, nativeQuery = true)
    List<WeeklyPriceOHLC> findPreviousTwoWeeklyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT w FROM WeeklyPriceOHLC w WHERE w.ticker = :ticker AND w.startDate < :date")
    List<WeeklyPriceOHLC> findWeeklyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT w FROM WeeklyPriceOHLC w WHERE w.ticker = :ticker AND w.startDate = :date")
    List<WeeklyPriceOHLC> findWeeklyByTickerAndStartDate(String ticker, LocalDate date);

    @Query(value = """
            SELECT id, ticker, start_date, end_date, high, low, open, close, performance
            FROM (
                SELECT id, ticker, start_date, end_date, high, low, open, close, performance,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS rn
                FROM monthly_prices
                WHERE ticker IN (:tickers)
            ) AS subquery
            WHERE rn <= 2
            ORDER BY ticker, start_date DESC;
            """, nativeQuery = true)
    List<MonthlyPriceOHLC> findPreviousTwoMonthlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT m FROM MonthlyPriceOHLC m WHERE m.ticker = :ticker AND m.startDate < :date")
    List<MonthlyPriceOHLC> findMonthlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT m FROM MonthlyPriceOHLC m WHERE m.ticker = :ticker AND m.startDate = :date")
    List<MonthlyPriceOHLC> findMonthlyByTickerAndStartDate(String ticker, LocalDate date);

    @Query(value = """
            SELECT id, ticker, start_date, end_date, high, low, open, close, performance
            FROM (
                SELECT id, ticker, start_date, end_date, high, low, open, close, performance,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS rn
                FROM yearly_prices
                WHERE ticker IN (:tickers)
            ) AS subquery
            WHERE rn <= 2
            ORDER BY ticker, start_date DESC;
            """, nativeQuery = true)
    List<YearlyPriceOHLC> findPreviousTwoYearlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT y FROM YearlyPriceOHLC y WHERE y.ticker = :ticker AND y.startDate < :date")
    List<YearlyPriceOHLC> findYearlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT y FROM YearlyPriceOHLC y WHERE y.ticker = :ticker AND y.startDate = :date")
    List<YearlyPriceOHLC> findYearlyByTickerAndStartDate(String ticker, LocalDate date);
}