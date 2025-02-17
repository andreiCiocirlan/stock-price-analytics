package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;

import java.util.List;


@Repository
public interface PricesRepository extends JpaRepository<AbstractPrice, Long> {

    @Query(value = """
            WITH daily_grouped AS (
                SELECT
                    ticker,
                    DATE_TRUNC('week', date)::date AS week_start,
                    open,
                    ROW_NUMBER() OVER (PARTITION BY ticker, DATE_TRUNC('week', date) ORDER BY date) AS rn
                FROM
                    daily_prices
                WHERE date >= '2022-01-01' and CURRENT_DATE
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
}