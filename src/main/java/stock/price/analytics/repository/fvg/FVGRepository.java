package stock.price.analytics.repository.fvg;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.fvg.FairValueGap;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FVGRepository extends JpaRepository<FairValueGap, Long> {

    @Query(value = """
            SELECT * FROM fvg WHERE timeframe = :timeframe AND status = 'OPEN';
            """, nativeQuery = true)
    List<FairValueGap> findByTimeframeAndStatusOpen(@Param(value = "timeframe") String timeframe);

    @Query(value = """
            WITH price_data AS (
                SELECT
            		ticker,
                    date_trunc('year', start_date)::date AS wmy_date,
                    open, high, low, close,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date) AS rn
                FROM yearly_prices
                WHERE start_date >= :date
            ),
            fvg_candidates AS (
                SELECT a.ticker,
                    a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
            		a.high AS high1, b.high AS high2, c.high AS high3,
                    a.low AS low1, b.low AS low2, c.low AS low3
                FROM price_data a
                JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
            ),
            identified_fvgs AS (
                SELECT
                    *,
                    CASE
                        WHEN low1 > high3 THEN 'BEARISH'
                        WHEN high1 < low3 THEN 'BULLISH'
                    END AS type
                FROM fvg_candidates
                WHERE (low1 > high3 OR high1 < low3)
            )
            SELECT
                nextval('sequence_fvg') as id,
            	'YEARLY' as timeframe,
                'OPEN' as status,
            	fvg.ticker as ticker,
            	fvg.date2 as date,
                fvg.type,
                CASE
                    WHEN low1 > high3 THEN low1
                    WHEN high1 < low3 THEN high1
                END AS low,
                CASE
                    WHEN low1 > high3 THEN high3
                    WHEN high1 < low3 THEN low3
                END AS high
            FROM identified_fvgs fvg
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM price_data next_wmy
                    WHERE next_wmy.wmy_date > fvg.date3 AND next_wmy.ticker = fvg.ticker
                      AND (
                          (fvg.type = 'BULLISH' AND next_wmy.low < fvg.high1)
                          OR (fvg.type = 'BEARISH' AND next_wmy.high > fvg.low1)
                      )
                )
            order by ticker, fvg.date2 desc;
            """, nativeQuery = true)
    List<FairValueGap> findAllYearlyFVGsAfter(LocalDate date);

    @Query(value = """
            WITH price_data AS (
                SELECT
            		ticker,
                    date_trunc('quarter', start_date)::date AS wmy_date,
                    open, high, low, close,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date) AS rn
                FROM quarterly_prices
                WHERE start_date >= :date
            ),
            fvg_candidates AS (
                SELECT a.ticker,
                    a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
            		a.high AS high1, b.high AS high2, c.high AS high3,
                    a.low AS low1, b.low AS low2, c.low AS low3
                FROM price_data a
                JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
            ),
            identified_fvgs AS (
                SELECT
                    *,
                    CASE
                        WHEN low1 > high3 THEN 'BEARISH'
                        WHEN high1 < low3 THEN 'BULLISH'
                    END AS type
                FROM fvg_candidates
                WHERE (low1 > high3 OR high1 < low3)
            )
            SELECT
                nextval('sequence_fvg') as id,
            	'QUARTERLY' as timeframe,
                'OPEN' as status,
            	fvg.ticker as ticker,
            	fvg.date2 as date,
                fvg.type,
                CASE
                    WHEN low1 > high3 THEN low1
                    WHEN high1 < low3 THEN high1
                END AS low,
                CASE
                    WHEN low1 > high3 THEN high3
                    WHEN high1 < low3 THEN low3
                END AS high
            FROM identified_fvgs fvg
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM price_data next_wmy
                    WHERE next_wmy.wmy_date > fvg.date3 AND next_wmy.ticker = fvg.ticker
                      AND (
                          (fvg.type = 'BULLISH' AND next_wmy.low < fvg.high1)
                          OR (fvg.type = 'BEARISH' AND next_wmy.high > fvg.low1)
                      )
                )
            order by ticker, fvg.date2 desc;
            """, nativeQuery = true)
    List<FairValueGap> findAllQuarterlyFVGsAfter(LocalDate date);

    @Query(value = """
            WITH price_data AS (
                SELECT
            		ticker,
                    date_trunc('month', start_date)::date AS wmy_date,
                    open, high, low, close,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date) AS rn
                FROM monthly_prices
                WHERE start_date >= :date
            ),
            fvg_candidates AS (
                SELECT a.ticker,
                    a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
            		a.high AS high1, b.high AS high2, c.high AS high3,
                    a.low AS low1, b.low AS low2, c.low AS low3
                FROM price_data a
                JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
            ),
            identified_fvgs AS (
                SELECT
                    *,
                    CASE
                        WHEN low1 > high3 THEN 'BEARISH'
                        WHEN high1 < low3 THEN 'BULLISH'
                    END AS type
                FROM fvg_candidates
                WHERE (low1 > high3 OR high1 < low3)
            )
            SELECT
                nextval('sequence_fvg') as id,
                'MONTHLY' as timeframe,
                'OPEN' as status,
            	fvg.ticker as ticker,
            	fvg.date2 as date,
                fvg.type,
                CASE
                    WHEN low1 > high3 THEN low1
                    WHEN high1 < low3 THEN high1
                END AS low,
                CASE
                    WHEN low1 > high3 THEN high3
                    WHEN high1 < low3 THEN low3
                END AS high
            FROM identified_fvgs fvg
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM price_data next_wmy
                    WHERE next_wmy.wmy_date > fvg.date3 AND next_wmy.ticker = fvg.ticker
                      AND (
                          (fvg.type = 'BULLISH' AND next_wmy.low < fvg.high1)
                          OR (fvg.type = 'BEARISH' AND next_wmy.high > fvg.low1)
                      )
                )
            order by ticker, fvg.date2 desc;
            """, nativeQuery = true)
    List<FairValueGap> findAllMonthlyFVGsAfter(LocalDate date);

    @Query(value = """
            WITH price_data AS (
                SELECT
            		ticker,
                    date_trunc('week', start_date)::date AS wmy_date,
                    open, high, low, close,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date) AS rn
                FROM weekly_prices
                WHERE start_date >= :date
            ),
            fvg_candidates AS (
                SELECT a.ticker,
                    a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
            		a.high AS high1, b.high AS high2, c.high AS high3,
                    a.low AS low1, b.low AS low2, c.low AS low3
                FROM price_data a
                JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
            ),
            identified_fvgs AS (
                SELECT
                    *,
                    CASE
                        WHEN low1 > high3 THEN 'BEARISH'
                        WHEN high1 < low3 THEN 'BULLISH'
                    END AS type
                FROM fvg_candidates
                WHERE (low1 > high3 OR high1 < low3)
            )
            SELECT
                nextval('sequence_fvg') as id,
            	'WEEKLY' as timeframe,
                'OPEN' as status,
            	fvg.ticker as ticker,
            	fvg.date2 as date,
                fvg.type,
                CASE
                    WHEN low1 > high3 THEN low1
                    WHEN high1 < low3 THEN high1
                END AS low,
                CASE
                    WHEN low1 > high3 THEN high3
                    WHEN high1 < low3 THEN low3
                END AS high
            FROM identified_fvgs fvg
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM price_data next_wmy
                    WHERE next_wmy.wmy_date > fvg.date3 AND next_wmy.ticker = fvg.ticker
                      AND (
                          (fvg.type = 'BULLISH' AND next_wmy.low < fvg.high1)
                          OR (fvg.type = 'BEARISH' AND next_wmy.high > fvg.low1)
                      )
                )
            order by ticker, fvg.date2 desc;
            """, nativeQuery = true)
    List<FairValueGap> findAllWeeklyFVGsAfter(LocalDate date);

    @Query(value = """
            WITH price_data AS (
                SELECT
            		ticker,
                    date AS wmy_date,
                    open, high, low, close,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date) AS rn
                FROM daily_prices
                WHERE date >= :date
            ),
            fvg_candidates AS (
                SELECT a.ticker,
                    a.wmy_date AS date1, b.wmy_date AS date2, c.wmy_date AS date3,
            		a.high AS high1, b.high AS high2, c.high AS high3,
                    a.low AS low1, b.low AS low2, c.low AS low3
                FROM price_data a
                JOIN price_data b ON b.rn = a.rn + 1 AND b.ticker = a.ticker
                JOIN price_data c ON c.rn = a.rn + 2 AND c.ticker = a.ticker
            ),
            identified_fvgs AS (
                SELECT
                    *,
                    CASE
                        WHEN low1 > high3 THEN 'BEARISH'
                        WHEN high1 < low3 THEN 'BULLISH'
                    END AS type
                FROM fvg_candidates
                WHERE (low1 > high3 OR high1 < low3)
            )
            SELECT
                nextval('sequence_fvg') as id,
            	'DAILY' as timeframe,
                'OPEN' as status,
            	fvg.ticker as ticker,
            	fvg.date2 as date,
                fvg.type,
                CASE
                    WHEN low1 > high3 THEN low1
                    WHEN high1 < low3 THEN high1
                END AS low,
                CASE
                    WHEN low1 > high3 THEN high3
                    WHEN high1 < low3 THEN low3
                END AS high
            FROM identified_fvgs fvg
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM price_data next_wmy
                    WHERE next_wmy.wmy_date > fvg.date3 AND next_wmy.ticker = fvg.ticker
                      AND (
                          (fvg.type = 'BULLISH' AND next_wmy.low < fvg.high1)
                          OR (fvg.type = 'BEARISH' AND next_wmy.high > fvg.low1)
                      )
                )
            order by ticker, fvg.date2 desc;
            """, nativeQuery = true)
    List<FairValueGap> findAllDailyFVGsAfter(LocalDate date);

    @Modifying
    @Transactional
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

}
