package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.util.Constants;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickerService {

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional
    public void renameTicker(String oldTicker, String newTicker) {
        for (String table : Constants.DB_TABLES) {
            String column = "daily_prices_json".equals(table) ? "symbol" : "ticker";
            String sql = STR."UPDATE \{table} SET \{column} = ?2 WHERE \{column} = ?1";

            int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, oldTicker)
                    .setParameter(2, newTicker)
                    .executeUpdate();
            log.info("Ticker rename: updated {} rows for {} ", rowsAffected, table);
        }
    }

    @Transactional
    public void deleteAllDataFor(String ticker) {
        for (String table : Constants.DB_TABLES) {
            String column = "daily_prices_json".equals(table) ? "symbol" : "ticker";
            String sql = STR."DELETE FROM \{table} WHERE \{column} = ?1";

            int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, ticker)
                    .executeUpdate();
            log.info("deleted {} rows for {} ", rowsAffected, table);
        }
    }

}