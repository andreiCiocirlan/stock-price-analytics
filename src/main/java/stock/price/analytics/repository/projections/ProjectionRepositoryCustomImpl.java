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
                         tlb.low AS level_1,
                         sp.second_point_max_high AS level_0,
                         ABS(sp.second_point_max_high - tlb.low) AS diff,
                         sp.second_point_max_high + ABS(sp.second_point_max_high - tlb.low) * 1 AS level_minus1,
                         sp.second_point_max_high + ABS(sp.second_point_max_high - tlb.low) * 2 AS level_minus2,
                         sp.second_point_max_high + ABS(sp.second_point_max_high - tlb.low) * 2.5 AS level_minus2_5,
                         sp.second_point_max_high + ABS(sp.second_point_max_high - tlb.low) * 4 AS level_minus4,
                         sp.second_point_max_high + ABS(sp.second_point_max_high - tlb.low) * 4.5 AS level_minus4_5
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
                        AND dp.close >= s.highest * 0.85) -- close within 15% of all-time high
                        AND (dp.ticker = :ticker))
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
                        ROW_NUMBER() OVER (PARTITION BY lt.ticker ORDER BY lt.high DESC) AS top_rank
                      FROM local_tops lt
                    ),
                    top3_local_tops AS (
                      SELECT * FROM ranked_local_tops WHERE top_rank <= 3
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
                        AND rp.low < tlt.high  -- second point must be below local top high
                    )
                    SELECT
                      tlt.ticker,
                      tlt.date AS local_top_date,
                      sp.second_point_date,
                      sp.second_point_min_low AS level_1,
                      tlt.high AS level_0,
                      ABS(tlt.high - sp.second_point_min_low) AS diff,
                      sp.second_point_min_low - ABS(tlt.high - sp.second_point_min_low) * 1 AS level_plus1,
                      sp.second_point_min_low - ABS(tlt.high - sp.second_point_min_low) * 2 AS level_plus2,
                      sp.second_point_min_low - ABS(tlt.high - sp.second_point_min_low) * 2.5 AS level_plus2_5,
                      sp.second_point_min_low - ABS(tlt.high - sp.second_point_min_low) * 4 AS level_plus4,
                      sp.second_point_min_low - ABS(tlt.high - sp.second_point_min_low) * 4.5 AS level_plus4_5
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
            LocalDate firstPointDate = ((java.sql.Date) row[1]).toLocalDate();
            LocalDate secondPointDate = ((java.sql.Date) row[2]).toLocalDate();
            double level1 = ((Number) row[3]).doubleValue();
            double level0 = ((Number) row[4]).doubleValue();
            double diff = ((Number) row[5]).doubleValue();
            double level_minus1 = ((Number) row[6]).doubleValue();
            double level_minus2 = ((Number) row[7]).doubleValue();
            double level_minus2_5 = ((Number) row[8]).doubleValue();
            double level_minus4 = ((Number) row[9]).doubleValue();
            double level_minus4_5 = ((Number) row[10]).doubleValue();

            return new StandardDeviationProjectionDTO(
                    ticker,
                    firstPointDate,
                    secondPointDate,
                    diff,
                    level0,
                    level1,
                    level_minus1,
                    level_minus2,
                    level_minus2_5,
                    level_minus4,
                    level_minus4_5
            );
        }).collect(Collectors.toList());
    }

}
