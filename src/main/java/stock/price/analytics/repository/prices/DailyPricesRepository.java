package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.DailyPrice;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface DailyPricesRepository extends JpaRepository<DailyPrice, Long> {

    long countByDateBefore(LocalDate date);
    long countByDateAfter(LocalDate date);
    long countByDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyPrice> findByTickerIn(List<String> tickerList);
    List<DailyPrice> findByDate(LocalDate date);
    List<DailyPrice> findByDateBefore(LocalDate date);
    List<DailyPrice> findByDateAfterOrderByDate(LocalDate date);
    List<DailyPrice> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query(value = """
            WITH latest_prices AS (
                SELECT ticker, MAX(date) AS max_date
                FROM daily_prices
                WHERE date >= CURRENT_DATE - interval '5 days'
                GROUP BY ticker
            )
            SELECT dp.*
            FROM daily_prices dp
            JOIN latest_prices lp ON dp.ticker = lp.ticker AND dp.date = lp.max_date
            """, nativeQuery = true)
    List<DailyPrice> findLatestDailyPrices();

    List<DailyPrice> findByTickerAndDateLessThan(String ticker, LocalDate date);

    List<DailyPrice> findByTickerAndDate(String ticker, LocalDate date);

}
