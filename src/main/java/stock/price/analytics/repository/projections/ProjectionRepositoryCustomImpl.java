package stock.price.analytics.repository.projections;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.dto.StandardDeviationProjectionDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ProjectionRepositoryCustomImpl implements ProjectionRepositoryCustom {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<StandardDeviationProjectionDTO> findLast3BottomProjections(String ticker) {
        String sql = """
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
                        AND dp.close <= s.lowest * 1.15) AND (dp.ticker = :ticker))
                        AND dp.date BETWEEN current_date - interval '52 week' AND current_date
                    ),
                    local_bottoms AS (
                      SELECT
                        rp.*
                      FROM ranked_prices rp
                      WHERE rp.low < COALESCE((
                        SELECT MIN(low)
                        FROM ranked_prices
                        WHERE ticker = rp.ticker
                          AND rn BETWEEN rp.rn - 3 AND rp.rn + 3
                          AND rn <> rp.rn
                      ), 999999999)
                    ),
                    ranked_local_bottoms AS (
                      SELECT
                        lb.*,
                        ROW_NUMBER() OVER (PARTITION BY lb.ticker ORDER BY lb.low ASC) AS bottom_rank
                      FROM local_bottoms lb
                    ),
                    top3_local_bottoms AS (
                      SELECT * FROM ranked_local_bottoms WHERE bottom_rank <= 3
                    ),
                    second_points AS (
                      SELECT
                        tlb.ticker,
                        tlb.rn,
                        rp.date AS second_point_date,
                        rp.high AS second_point_max_high,
                        ROW_NUMBER() OVER (
                          PARTITION BY tlb.ticker, tlb.rn
                          ORDER BY rp.high DESC, rp.date ASC
                        ) AS rn_rank
                      FROM top3_local_bottoms tlb
                      JOIN ranked_prices rp ON rp.ticker = tlb.ticker
                        AND rp.rn BETWEEN tlb.rn - 3 AND tlb.rn + 3
                        AND rp.rn <> tlb.rn
                        AND rp.high > tlb.low  -- second point must be above local bottom low
                    )
                    SELECT
                      tlb.ticker,
                      tlb.date AS local_bottom_date,
                      sp.second_point_date,
                      tlb.low AS level_0,
                      tlb.low + (sp.second_point_max_high - tlb.low) * 1 AS level_1,
                      ABS(sp.second_point_max_high - tlb.low) AS diff
                    FROM top3_local_bottoms tlb
                    JOIN second_points sp ON sp.ticker = tlb.ticker AND sp.rn = tlb.rn
                    WHERE sp.rn_rank = 1
                    ORDER BY tlb.ticker, tlb.date DESC;
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ticker", ticker);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToDTO(results);
    }

    @Override
    public List<StandardDeviationProjectionDTO> findLast3TopProjections(String ticker) {
        String sql = """
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
                          AND s.highest <= dp.close * 1.15) AND (dp.ticker = :ticker))
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
                    ranked_local_tops AS (
                      SELECT
                        lt.*,
                        ROW_NUMBER() OVER (PARTITION BY lt.ticker ORDER BY lt.high DESC) AS peak_rank
                      FROM local_tops lt
                    ),
                    top3_local_tops AS (
                      SELECT * FROM ranked_local_tops WHERE peak_rank <= 3
                    ),
                    second_points AS (
                      SELECT
                        tlt.ticker,
                        tlt.rn,
                        rp.date AS second_point_date,
                        rp.low AS second_point_min_low,
                        ROW_NUMBER() OVER (
                          PARTITION BY tlt.ticker, tlt.rn
                          ORDER BY rp.low ASC, rp.date ASC
                        ) AS rn_rank
                      FROM top3_local_tops tlt
                      JOIN ranked_prices rp ON rp.ticker = tlt.ticker
                        AND rp.rn BETWEEN tlt.rn - 3 AND tlt.rn + 3
                        AND rp.rn <> tlt.rn
                    )
                    SELECT
                      tlt.ticker,
                      tlt.date AS local_top_date,
                      sp.second_point_date,
                      tlt.high AS level_0,
                      tlt.high + (sp.second_point_min_low - tlt.high) * 1 AS level_1,
                      ABS(sp.second_point_min_low - tlt.high) AS diff
                    FROM top3_local_tops tlt
                    JOIN second_points sp ON sp.ticker = tlt.ticker AND sp.rn = tlt.rn
                    WHERE sp.rn_rank = 1
                    ORDER BY tlt.ticker, tlt.date DESC;
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
            double level1 = ((Number) row[3]).doubleValue();
            double level0 = ((Number) row[4]).doubleValue();
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
