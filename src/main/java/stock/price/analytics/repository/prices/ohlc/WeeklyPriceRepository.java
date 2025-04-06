package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stock.price.analytics.model.prices.ohlc.WeeklyPrice;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyPriceRepository extends JpaRepository<WeeklyPrice, Long> {

    @Query(value = """
                SELECT *
                FROM weekly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '2 week') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<WeeklyPrice> findPreviousThreeWeeklyPricesForTickers(@Param("tickers") List<String> tickers);

    List<WeeklyPrice> findByTickerAndStartDateLessThanEqual(String ticker, LocalDate date);

}
