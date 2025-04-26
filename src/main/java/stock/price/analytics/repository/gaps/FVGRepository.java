package stock.price.analytics.repository.gaps;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.gaps.FairValueGap;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FVGRepository extends JpaRepository<FairValueGap, Long> {

    @Query(value = """
            SELECT * FROM fvg WHERE timeframe = :timeframe;
            """, nativeQuery = true)
    List<FairValueGap> findByTimeframe(@Param(value = "timeframe") String timeframe);

    @Modifying
    @Query(value = """
            UPDATE fvg
                   SET high = high * :multiplier,
                   low = low * :multiplier
                           WHERE
                   ticker = :ticker AND
                   CASE
                   WHEN timeframe = 'DAILY' THEN date <= :stockSplitDate
                   WHEN timeframe = 'WEEKLY' THEN date_trunc('week', date) <= CAST(:stockSplitDate AS date)
                   WHEN timeframe = 'MONTHLY' THEN date_trunc('month', date) <= CAST(:stockSplitDate AS date)
                   WHEN timeframe = 'QUARTERLY' THEN date_trunc('quarter', date) <= CAST(:stockSplitDate AS date)
                   WHEN timeframe = 'YEARLY' THEN date_trunc('year', date) <= CAST(:stockSplitDate AS date)
                   ELSE FALSE
                   END;
            """, nativeQuery = true)
    int updateFVGPricesForStockSplit(@Param(value = "ticker") String ticker, @Param(value = "stockSplitDate") LocalDate  stockSplitDate, @Param(value = "multiplier") double multiplier);

    @Query(value = """
            SELECT id, 'daily_date_discrepancy' FROM FVG
            WHERE timeframe = 'DAILY' AND EXTRACT(DOW FROM DATE) IN (6, 7) -- daily fvg date cannot be on saturday/sunday
                UNION ALL
            select id, 'weekly_date_discrepancy' from fvg
            WHERE timeframe = 'WEEKLY' AND extract(DOW from DATE) <> 1 -- weekly fvg date must be Monday
                UNION ALL
            select id, 'monthly_date_discrepancy' from fvg
            WHERE timeframe = 'MONTHLY' AND extract(DAY from DATE) <> 1 -- monthly fvg date must be 1st of the month
                UNION ALL
            select id, 'quarterly_date_discrepancy' from fvg
            WHERE timeframe = 'QUARTERLY' AND (extract(day from DATE) <> 1 OR extract(MONTH from DATE) NOT IN (1, 4, 7, 10)) -- quarterly fvg date must be 1st of Jan/Apr/Jul/Oct
                UNION ALL
            select id, 'yearly_date_discrepancy' from fvg
            WHERE timeframe = 'YEARLY' AND (extract(day from DATE) <> 1 OR extract(MONTH from DATE) <> 1) -- yearly fvg date must be Jan 1st
            """, nativeQuery = true)
    List<Object[]> findFvgDateDiscrepancies();

}
