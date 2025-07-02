package stock.price.analytics.repository.prices.highlow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.highlow.HighLowForPeriod;
import stock.price.analytics.model.prices.highlow.TickerHighLowView;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HighLowForPeriodRepository extends JpaRepository<HighLowForPeriod, Long> {

    @Query(value = """
            WITH latest_prices AS (
                SELECT
                    wp.ticker,
                    date,
                    wp.high,
                    wp.low,
                    ROW_NUMBER() OVER (PARTITION BY wp.ticker ORDER BY wp.date DESC) AS rn
                FROM weekly_prices wp
                WHERE
                    wp.date BETWEEN (CAST(:tradingDate AS DATE) - INTERVAL '75 weeks') AND CAST(:tradingDate AS DATE)  -- Adjust to get the last 75 weeks (buffer zone for inactive tickers between periods)
            	and wp.ticker in (select ticker from stocks where xtb_stock = true and delisted_date is null)
            ),
            filtered_prices AS (
                SELECT
                    lp.ticker,
                    lp.date,
                    lp.high,
                    lp.low
                FROM latest_prices lp
                WHERE lp.rn <= :week_count  -- Get only the latest 4, 52 rows per ticker
            )
            SELECT
                fp.ticker as ticker,
                MIN(fp.low) AS low,
                MAX(fp.high) AS high
            FROM filtered_prices fp
            GROUP BY fp.ticker
            ORDER BY fp.ticker;
            """, nativeQuery = true)
    List<TickerHighLowView> highLowPricesInPastWeeks(@Param(value = "tradingDate") LocalDate tradingDate, @Param(value = "week_count") Integer week_count);

    @Query(value = """
            select
                date_trunc('week', wp.date)::date AS date,
                SUM(CASE WHEN wp.high > hl_all_prev.high THEN 1 END) AS new_highest,
                SUM(CASE WHEN wp.high > hl52w_prev.high THEN 1 END) AS new_high_52w,
                SUM(CASE WHEN wp.high > hl4w_prev.high THEN 1 END) AS new_high_4w,
                SUM(CASE WHEN wp.low < hl_all_prev.low THEN 1 END) AS new_lowest,
                SUM(CASE WHEN wp.low < hl52w_prev.low THEN 1 END) AS new_low_52w,
                SUM(CASE WHEN wp.low < hl4w_prev.low THEN 1 END) AS new_low_4w
            FROM weekly_prices wp
                join highest_lowest hl_all_prev on hl_all_prev.ticker = wp.ticker AND hl_all_prev.date = wp.date - INTERVAL '7 days'
                join high_low52w hl52w_prev on hl52w_prev.ticker = wp.ticker AND  hl52w_prev.date = wp.date - INTERVAL '7 days'
                JOIN high_low4w hl4w_prev ON hl4w_prev.ticker = wp.ticker AND hl4w_prev.date = wp.date - INTERVAL '7 days'
            WHERE
                (wp.date between (CURRENT_DATE - INTERVAL '7 days') and CURRENT_DATE) AND
                wp.ticker in (select ticker from stocks where xtb_stock = true)\s
            GROUP BY date_trunc('week', wp.date)
            ORDER BY date_trunc('week', wp.date) DESC;
            """, nativeQuery = true)
    List<Object[]> newHighLowsThisWeek();

}
