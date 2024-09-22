package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Previous52WPricesService {

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Run once after weekly_prices populated (afterward update with incoming data)
     */
    public void insertPrev52w() {
        String query = queryInsertPrev52w();
        int insertCount = entityManager.createNativeQuery(query).executeUpdate();
        if (insertCount != 0) {
            log.warn("inserted {} rows for Prev52w", insertCount);
        }
    }

    private String queryInsertPrev52w() {
        return """
                INSERT INTO PREV_52W
                SELECT nextval('sequence_prices') AS id, close, end_date, high, low, open, performance, start_date, ticker  FROM
                (
                	SELECT  close, end_date, high, low, open, performance, start_date, ticker
                	FROM (
                	    SELECT ticker, start_date, end_date, high, low, open, close, performance,
                	        ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS rn
                	    FROM weekly_prices
                	) AS subquery
                	WHERE rn <= 52
                	ORDER BY ticker, start_date DESC
                )
                """;
    }

}
