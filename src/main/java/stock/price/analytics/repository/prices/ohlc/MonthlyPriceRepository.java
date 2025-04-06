package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stock.price.analytics.model.prices.ohlc.MonthlyPrice;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyPriceRepository extends JpaRepository<MonthlyPrice, Long> {

    List<MonthlyPrice> findByTickerAndStartDateBetween(String ticker, LocalDate from, LocalDate to);

    @Query(value = """
                SELECT *
                FROM monthly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 month') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<MonthlyPrice> findPreviousThreeMonthlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT m FROM MonthlyPrice m WHERE m.ticker = :ticker AND m.startDate < :date")
    List<MonthlyPrice> findMonthlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT m FROM MonthlyPrice m WHERE m.ticker = :ticker AND m.startDate = :date")
    List<MonthlyPrice> findMonthlyByTickerAndStartDate(String ticker, LocalDate date);

}
