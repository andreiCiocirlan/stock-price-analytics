package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.json.DailyPricesJSON;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyPricesJSONRepository extends JpaRepository<DailyPricesJSON, Long> {

    @Query(value = "SELECT * from daily_prices_json where date between :from and :to", nativeQuery = true)
    List<DailyPricesJSON> findByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);


}