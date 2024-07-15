package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface DailyPriceOHLCRepository extends JpaRepository<DailyPriceOHLC, Long> {

    long countByDateBefore(LocalDate date);
    long countByDateAfter(LocalDate date);
    long countByDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyPriceOHLC> findByDateBefore(LocalDate date);
    List<DailyPriceOHLC> findByDateAfter(LocalDate date);
    List<DailyPriceOHLC> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * from latest_prices", nativeQuery = true)
    List<DailyPriceOHLC> findLatestByTicker();

    @Query(value ="""
            select lp.* from latest_prices lp
            where lp.date >= :date
    """, nativeQuery = true)
    List<DailyPriceOHLC> findAllLatestByTickerWithDateAfter(LocalDate date); // ALL tickers

    @Query(value ="""
            select lp.* from latest_prices lp
            join stocks s on s.ticker = lp.ticker and s.xtb_stock = true
            where lp.date >= :date
    """, nativeQuery = true)
    List<DailyPriceOHLC> findXTBLatestByTickerWithDateAfter(LocalDate date); // XTB only
}
