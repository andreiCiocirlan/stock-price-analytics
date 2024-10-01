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

    List<DailyPriceOHLC> findByTickerAndDateLessThan(String ticker, LocalDate date);

    List<DailyPriceOHLC> findByTickerAndDate(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM weekly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '2 week') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<WeeklyPriceOHLC> findPreviousThreeWeeksPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT w FROM WeeklyPriceOHLC w WHERE w.ticker = :ticker AND w.startDate < :date")
    List<WeeklyPriceOHLC> findWeeklyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT w FROM WeeklyPriceOHLC w WHERE w.ticker = :ticker AND w.startDate = :date")
    List<WeeklyPriceOHLC> findWeeklyByTickerAndStartDate(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM monthly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 week') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<MonthlyPriceOHLC> findPreviousThreeMonthlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT m FROM MonthlyPriceOHLC m WHERE m.ticker = :ticker AND m.startDate < :date")
    List<MonthlyPriceOHLC> findMonthlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT m FROM MonthlyPriceOHLC m WHERE m.ticker = :ticker AND m.startDate = :date")
    List<MonthlyPriceOHLC> findMonthlyByTickerAndStartDate(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM yearly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 week') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<YearlyPriceOHLC> findPreviousThreeYearlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT y FROM YearlyPriceOHLC y WHERE y.ticker = :ticker AND y.startDate < :date")
    List<YearlyPriceOHLC> findYearlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT y FROM YearlyPriceOHLC y WHERE y.ticker = :ticker AND y.startDate = :date")
    List<YearlyPriceOHLC> findYearlyByTickerAndStartDate(String ticker, LocalDate date);
}