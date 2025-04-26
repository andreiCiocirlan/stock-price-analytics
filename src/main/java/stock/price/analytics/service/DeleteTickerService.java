package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteTickerService {

    @PersistenceContext
    private final EntityManager entityManager;

    private static final List<String> DB_TABLES = List.of(
            "daily_prices_json",
            "daily_prices",
            "weekly_prices",
            "monthly_prices",
            "quarterly_prices",
            "yearly_prices",
            "stocks",
            "price_gaps",
            "fvg",
            "high_low4w",
            "high_low52w",
            "highest_lowest"
    );


    @Transactional
    public void deleteAllDataFor(String ticker) {
        for (String table : DB_TABLES) {
            String column = "daily_prices_json".equals(table) ? "symbol" : "ticker";
            String sql = STR."DELETE FROM \{table} WHERE \{column} = ?1";

            int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, ticker)
                    .executeUpdate();
            log.info("deleted {} rows for {} ", rowsAffected, table);
        }
    }

}