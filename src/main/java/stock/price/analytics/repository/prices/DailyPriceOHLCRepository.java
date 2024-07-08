package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
