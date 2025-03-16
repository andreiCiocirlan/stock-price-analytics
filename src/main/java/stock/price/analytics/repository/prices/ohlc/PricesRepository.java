package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;


@Repository
public interface PricesRepository extends JpaRepository<AbstractPrice, Long> {


}