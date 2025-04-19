package stock.price.analytics.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteTickerService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final String[][] deletionTargets = {
            {"daily_prices_json", "symbol"},
            {"daily_prices", "ticker"},
            {"weekly_prices", "ticker"},
            {"monthly_prices", "ticker"},
            {"quarterly_prices", "ticker"},
            {"yearly_prices", "ticker"},
            {"stocks", "ticker"},
            {"price_gaps", "ticker"},
            {"fvg", "ticker"},
            {"high_low4w", "ticker"},
            {"high_low52w", "ticker"},
            {"highest_lowest", "ticker"}
    };

    @Transactional
    public void deleteAllDataFor(String ticker) {
        for (String[] target : deletionTargets) {
            String table = target[0];
            String column = target[1];
            String sql = STR."DELETE FROM \{table} WHERE \{column} = ?1";

            int rowsAffected = entityManager.createNativeQuery(sql)
                    .setParameter(1, ticker)
                    .executeUpdate();
            log.info("deleted {} rows for {} ", rowsAffected, table);
        }
    }

}