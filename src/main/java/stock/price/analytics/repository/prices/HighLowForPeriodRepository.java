package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;

@Repository
public interface HighLowForPeriodRepository extends JpaRepository<HighLowForPeriod, Long> {

    @Query(value = """
            UPDATE stocks s
            SET
                highest = hlp.high,
                lowest = hlp.low
            FROM (
                SELECT ticker, high, low
                FROM highest_lowest
                WHERE (ticker, start_date) IN (
                    SELECT ticker, MAX(start_date)
                    FROM highest_lowest
                    GROUP BY ticker
                )
            ) hlp
            WHERE s.ticker = hlp.ticker;
            """, nativeQuery = true)
    void updateHighestLowest();

    @Query(value = """
            UPDATE stocks s
            SET
                high52w = h52w.high52w,
                low52w = h52w.low52w
            FROM (
                SELECT ticker, high AS high52w, low AS low52w
                FROM high_low52w
                WHERE (ticker, start_date) IN (
                    SELECT ticker, MAX(start_date)
                    FROM high_low52w
                    GROUP BY ticker
                )
            ) h52w
            WHERE s.ticker = h52w.ticker;
            """, nativeQuery = true)
    void updateHighLow52w();

    @Query(value = """
            UPDATE stocks s
            SET
                high4w = h4w.high4w,
                low4w = h4w.low4w
            FROM (
                SELECT ticker, high AS high4w, low AS low4w
                FROM high_low4w
                WHERE (ticker, start_date) IN (
                    SELECT ticker, MAX(start_date)
                    FROM high_low4w
                    GROUP BY ticker
                )
            ) h4w
            WHERE s.ticker = h4w.ticker;
            """, nativeQuery = true)
    void updateHighLow4w();

}
