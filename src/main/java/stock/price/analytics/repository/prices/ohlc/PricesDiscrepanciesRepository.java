package stock.price.analytics.repository.prices.ohlc;

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

    @Modifying
    @Transactional
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
                select wp.ticker, md.month_start, md.open from m_daily md
                join monthly_prices wp on wp.ticker = md.ticker and wp.start_date = md.month_start
                where round(wp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE monthly_prices wp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE wp.ticker = pu.ticker AND wp.start_date = pu.month_start;
            """, nativeQuery = true)
    void updateMonthlyPricesWithOpeningPriceDiscrepancy();

    @Modifying
    @Transactional
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
                select wp.ticker, md.quarter_start, md.open from m_daily md
                join quarterly_prices wp on wp.ticker = md.ticker and wp.start_date = md.quarter_start
                where round(wp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE quarterly_prices wp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE wp.ticker = pu.ticker AND wp.start_date = pu.quarter_start;
            """, nativeQuery = true)
    void updateQuarterlyPricesWithOpeningPriceDiscrepancy();

    @Modifying
    @Transactional
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
                select wp.ticker, md.year_start, md.open from m_daily md
                join yearly_prices wp on wp.ticker = md.ticker and wp.start_date = md.year_start
                where round(wp.open::numeric, 2) <> round(md.open::numeric, 2)
            )
            UPDATE yearly_prices wp
            SET open = pu.open
            FROM PricesToUpdate pu
            WHERE wp.ticker = pu.ticker AND wp.start_date = pu.year_start;
            """, nativeQuery = true)
    void updateYearlyPricesWithOpeningPriceDiscrepancy();
}
