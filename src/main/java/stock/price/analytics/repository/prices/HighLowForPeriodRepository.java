package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;

@Repository
public interface HighLowForPeriodRepository extends JpaRepository<HighLowForPeriod, Long> {

}
