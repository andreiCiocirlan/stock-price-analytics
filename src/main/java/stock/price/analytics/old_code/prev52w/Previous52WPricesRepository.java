package stock.price.analytics.old_code.prev52w;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Previous52WPricesRepository extends JpaRepository<Previous52WPrices, Long> {

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW prev_52_weeks", nativeQuery = true)
    void refreshPrev52Weeks();

    @Query(value = """
            SELECT subq.id, subq.ticker, subq.start_date, subq.end_date, subq.high, subq.low, subq.open, subq.close, subq.performance
            FROM (
                SELECT p.*,
                    ROW_NUMBER() OVER (PARTITION BY p.ticker ORDER BY p.start_date DESC) AS rn
                FROM prev_52w p
                WHERE ticker IN (:tickers)
            ) AS subq
            WHERE rn <= 52
            ORDER BY subq.start_date DESC;
            """, nativeQuery = true)
    List<Previous52WPrices> prev52WeeksOrderedByStartDateDescForTickers(List<String> tickers);

}
