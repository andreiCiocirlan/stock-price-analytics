package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stock.price.analytics.model.prices.ohlc.WeeklyPrice;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyPricesRepository extends JpaRepository<WeeklyPrice, Long> {

    List<WeeklyPrice> findByStartDateBetween(LocalDate from, LocalDate to);

    @Query(value = """
                SELECT *
                FROM weekly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '2 week') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<WeeklyPrice> findPreviousThreeWeeklyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT w FROM WeeklyPrice w WHERE w.ticker = :ticker AND w.startDate < :date")
    List<WeeklyPrice> findWeeklyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT w FROM WeeklyPrice w WHERE w.ticker = :ticker AND w.startDate = :date")
    List<WeeklyPrice> findWeeklyByTickerAndStartDate(String ticker, LocalDate date);
}
