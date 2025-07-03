package stock.price.analytics.repository.projections;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stock.price.analytics.controller.dto.StandardDeviationProjectionDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProjectionRepositoryCustomImpl implements ProjectionRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<StandardDeviationProjectionDTO> findLast3ProjectionsByTicker(String ticker) {
        String sql = STR."""
                    WITH ranked_prices AS (
                        SELECT
                            dp.ticker,
                            dp.date,
                            dp.high,
                            dp.low,
                            dp.close,
                            ROW_NUMBER() OVER (PARTITION BY dp.ticker ORDER BY dp.date) AS rn
                        FROM daily_prices dp
                        JOIN stocks s ON dp.ticker = s.ticker
                        WHERE ((s.delisted_date IS NULL
                          AND s.highest <= dp.close * 1.05) AND (dp.ticker = :ticker))
                          AND dp.date BETWEEN current_date - interval '52 week' AND current_date
                    ),
                    local_tops AS (
                        SELECT
                            rp.*
                        FROM ranked_prices rp
                        WHERE rp.high > COALESCE((
                            SELECT MAX(high)
                            FROM ranked_prices
                            WHERE ticker = rp.ticker
                              AND rn BETWEEN rp.rn - 3 AND rp.rn + 3
                              AND rn <> rp.rn
                        ), 0)
                    ),
                    second_points AS (
                        SELECT
                            lt.ticker,
                            lt.rn,
                            rp.date AS second_point_date,
                            rp.low AS second_point_min_low,
                            ROW_NUMBER() OVER (
                                PARTITION BY lt.ticker, lt.rn
                                ORDER BY rp.low ASC, rp.date ASC
                            ) AS rn_rank
                        FROM local_tops lt
                        JOIN ranked_prices rp ON rp.ticker = lt.ticker
                            AND rp.rn BETWEEN lt.rn - 3 AND lt.rn + 3
                            AND rp.rn <> lt.rn
                    )
                    SELECT
                        lt.ticker,
                        lt.date AS local_top_date,
                        sp.second_point_date,
                        lt.high AS level_1, -- Renamed from local_top_high
                        lt.high + (sp.second_point_min_low - lt.high) * 1 AS level_0, -- Renamed from level_1
                        abs(sp.second_point_min_low - lt.high) AS diff -- Keep diff or derive step from level0 and level1
                    FROM local_tops lt
                    JOIN second_points sp ON sp.ticker = lt.ticker AND sp.rn = lt.rn
                    WHERE sp.rn_rank = 1
                    ORDER BY lt.date DESC
                    LIMIT 1;
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ticker", ticker);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToDTO(results);
    }

    public List<StandardDeviationProjectionDTO> mapToDTO(List<Object[]> results) {
        return results.stream().map(row -> {
            String ticker = (String) row[0];
            LocalDate localTopDate = ((java.sql.Date) row[1]).toLocalDate();
            LocalDate secondPointDate = ((java.sql.Date) row[2]).toLocalDate();
            double level0 = ((Number) row[3]).doubleValue();
            double level1 = ((Number) row[4]).doubleValue();
            double diff = ((Number) row[5]).doubleValue();

            return new StandardDeviationProjectionDTO(
                    ticker,
                    localTopDate,
                    secondPointDate,
                    diff,
                    level0,
                    level1
            );
        }).collect(Collectors.toList());
    }

}
