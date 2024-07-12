package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;
import stock.price.analytics.model.prices.ohlc.DailyPriceOHLC;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface PriceOHLCRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    List<DailyPriceOHLC> findByTickerAndDateBefore(String ticker, LocalDate date);
}