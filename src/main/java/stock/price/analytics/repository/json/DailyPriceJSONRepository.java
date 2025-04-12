package stock.price.analytics.repository.json;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.json.DailyPriceJSON;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyPriceJSONRepository extends JpaRepository<DailyPriceJSON, Long> {

    @Query(value = "SELECT * from daily_prices_json where date between :from and :to", nativeQuery = true)
    List<DailyPriceJSON> findByDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<DailyPriceJSON> findByDate(LocalDate date);

}