package stock.price.analytics.repository.prices.ohlc;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceDiscrepanciesRepository extends PriceRepository {

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
            select wp.ticker, 'weekly_opening_price_discrepancy'
            from weekly_prices wp
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
            select wp.ticker, 'weekly_high_low_discrepancy'
            from weekly_prices wp
            join daily_grouped pu on wp.ticker = pu.ticker AND wp.start_date = pu.start_date
                AND (round(wp.high::numeric, 2) <> round(pu.weekly_high::numeric, 2) or round(wp.low::numeric, 2) <> round(pu.weekly_low::numeric, 2))
            """, nativeQuery = true)
    List<Object[]> findWeeklyHighLowPriceDiscrepancies();

    @Modifying
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

    @Modifying
    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    DATE_TRUNC('month', date)::date AS month_start,
                    open,
                    ROW_NUMBER() OVER (PARTITION BY ticker, DATE_TRUNC('month', date) ORDER BY date) AS rn
                FROM
                    daily_prices
                WHERE date >=  date_trunc('month', CURRENT_DATE)
            ),
            m_daily as (
                SELECT ticker, month_start, open
                FROM daily_grouped
                WHERE rn = 1
            ),
            PricesToUpdate as (
                select mp.ticker, md.month_start, md.open from m_daily md
                join monthly_prices mp on mp.ticker = md.ticker and mp.start_date = md.month_start
                where round(mp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE monthly_prices mp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE mp.ticker = pu.ticker AND mp.start_date = pu.month_start;
            """, nativeQuery = true)
    void updateMonthlyPricesWithOpeningPriceDiscrepancy();

    @Modifying
    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    DATE_TRUNC('quarter', date)::date AS quarter_start,
                    open,
                    ROW_NUMBER() OVER (PARTITION BY ticker, DATE_TRUNC('quarter', date) ORDER BY date) AS rn
                FROM
                    daily_prices
                WHERE date >=  date_trunc('quarter', CURRENT_DATE)
            ),
            m_daily as (
                SELECT ticker, quarter_start, open
                FROM daily_grouped
                WHERE rn = 1
            ),
            PricesToUpdate as (
                select qp.ticker, md.quarter_start, md.open from m_daily md
                join quarterly_prices qp on qp.ticker = md.ticker and qp.start_date = md.quarter_start
                where round(qp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE quarterly_prices qp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE qp.ticker = pu.ticker AND qp.start_date = pu.quarter_start;
            """, nativeQuery = true)
    void updateQuarterlyPricesWithOpeningPriceDiscrepancy();

    @Modifying
    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    DATE_TRUNC('year', date)::date AS year_start,
                    open,
                    ROW_NUMBER() OVER (PARTITION BY ticker, DATE_TRUNC('year', date) ORDER BY date) AS rn
                FROM
                    daily_prices
                WHERE date >=  date_trunc('year', CURRENT_DATE)
            ),
            m_daily as (
                SELECT ticker, year_start, open
                FROM daily_grouped
                WHERE rn = 1
            ),
            PricesToUpdate as (
                select yp.ticker, md.year_start, md.open from m_daily md
                join yearly_prices yp on yp.ticker = md.ticker and yp.start_date = md.year_start
                where round(yp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE yearly_prices yp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE yp.ticker = pu.ticker AND yp.start_date = pu.year_start;
            """, nativeQuery = true)
    void updateYearlyPricesWithOpeningPriceDiscrepancy();
}
