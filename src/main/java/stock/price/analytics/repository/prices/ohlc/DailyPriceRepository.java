package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    List<DailyPrice> findByTickerAndDateLessThanEqual(String ticker, LocalDate date);

    @Query(value = """
                SELECT *
                FROM daily_prices
                WHERE date BETWEEN (DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '7 days') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, date DESC
            """, nativeQuery = true)
    List<DailyPrice> findDailyPricesForTickersFromLastWeekToDate(List<String> tickers);
}
