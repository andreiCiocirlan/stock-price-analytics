package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    @Query(value = """
            WITH latest_prices AS (
                SELECT *,
                       ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS row_num
                FROM daily_prices
                WHERE date >= CURRENT_DATE - interval '7 days'
            )
            SELECT id, open, high, low, close, ticker, date, performance
            FROM latest_prices
            WHERE row_num = 1;
            """, nativeQuery = true)
    List<DailyPrice> findLatestDailyPrices();

    List<DailyPrice> findByTickerAndDateLessThanEqual(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM daily_prices
                WHERE date BETWEEN (DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '7 days') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, date DESC
            """, nativeQuery = true)
    List<DailyPrice> findPreviousSevenDailyPricesForTickers(List<String> tickers);
}
