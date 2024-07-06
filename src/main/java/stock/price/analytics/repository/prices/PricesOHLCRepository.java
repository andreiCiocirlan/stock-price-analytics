package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.*;

import java.util.List;


@Repository
public interface PricesOHLCRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    @Query("SELECT p FROM DailyPriceOHLC p WHERE p.ticker = :ticker ORDER BY date DESC")
    List<DailyPriceOHLC> findDailyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM WeeklyPriceOHLC p WHERE p.ticker = :ticker ORDER BY startDate DESC")
    List<WeeklyPriceOHLC> findWeeklyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM MonthlyPriceOHLC p WHERE p.ticker = :ticker ORDER BY startDate DESC")
    List<MonthlyPriceOHLC> findMonthlyOHLCByTicker(@Param("ticker") String ticker);

    @Query("SELECT p FROM YearlyPriceOHLC p WHERE p.ticker = :ticker ORDER BY startDate DESC")
    List<YearlyPriceOHLC> findYearlyOHLCByTicker(@Param("ticker") String ticker);

}
