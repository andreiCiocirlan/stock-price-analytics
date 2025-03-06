package stock.price.analytics.repository.prices;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricesDiscrepanciesRepository extends PricesRepository {

    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    DATE_TRUNC('week', date)::date AS week_start,
                    open,
                    ROW_NUMBER() OVER (PARTITION BY ticker, DATE_TRUNC('week', date) ORDER BY date) AS rn
                FROM
                    daily_prices
                WHERE date >=  date_trunc('week', CURRENT_DATE)
            ),
            m_daily as (
                SELECT ticker, week_start, open
                FROM daily_grouped
                WHERE rn = 1
            ),
            PricesToUpdate as (
                select wp.ticker, md.week_start, md.open from m_daily md
                join weekly_prices wp on wp.ticker = md.ticker and wp.start_date = md.week_start
                where round(wp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            select wp.start_date, wp.ticker, wp.open as weekly_open, pu.open as correct_open from weekly_prices wp
            join PricesToUpdate pu on wp.ticker = pu.ticker AND wp.start_date = pu.week_start;
            """, nativeQuery = true)
    List<Object[]> findWeeklyOpeningPriceDiscrepancies();

    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    min(low) as weekly_low,
                    max(high) as weekly_high,
                    date_trunc('week', date) as start_date
                FROM
                    daily_prices
                WHERE date >= date_trunc('week', CURRENT_DATE)
                GROUP BY date_trunc('week', date), ticker
            )
            select wp.start_date, wp.ticker, round(wp.low::numeric, 2) as w_low, round(pu.weekly_low::numeric, 2) as d_low, round(wp.high::numeric, 2) as w_high, round(pu.weekly_high::numeric, 2) as d_high,
                wp.high <> pu.weekly_high as high_diff, pu.weekly_low <> wp.low as low_diff
            from weekly_prices wp
            join daily_grouped pu on wp.ticker = pu.ticker AND wp.start_date = pu.start_date
                AND (round(wp.high::numeric, 2) <> round(pu.weekly_high::numeric, 2) or round(wp.low::numeric, 2) <> round(pu.weekly_low::numeric, 2))
            """, nativeQuery = true)
    List<Object[]> findWeeklyHighLowPriceDiscrepancies();

    @Modifying
    @Transactional
    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    DATE_TRUNC('week', date)::date AS week_start,
                    open,
                    ROW_NUMBER() OVER (PARTITION BY ticker, DATE_TRUNC('week', date) ORDER BY date) AS rn
                FROM
                    daily_prices
                WHERE date >=  date_trunc('week', CURRENT_DATE)
            ),
            m_daily as (
                SELECT ticker, week_start, open
                FROM daily_grouped
                WHERE rn = 1
            ),
            PricesToUpdate as (
                select wp.ticker, md.week_start, md.open from m_daily md
                join weekly_prices wp on wp.ticker = md.ticker and wp.start_date = md.week_start
                where round(wp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE weekly_prices wp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE wp.ticker = pu.ticker AND wp.start_date = pu.week_start;
            """, nativeQuery = true)
    void updateWeeklyPricesWithOpeningPriceDiscrepancy();
}
