package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stock.price.analytics.model.prices.ohlc.MonthlyPrice;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyPriceRepository extends JpaRepository<MonthlyPrice, Long> {

    @Query(value = """
                SELECT *
                FROM monthly_prices
                WHERE date BETWEEN (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '2 month') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, date DESC
            """, nativeQuery = true)
    List<MonthlyPrice> findPreviousThreeMonthlyPricesForTickers(@Param("tickers") List<String> tickers);

    List<MonthlyPrice> findByTickerAndDateLessThanEqual(String ticker, LocalDate date);

}
