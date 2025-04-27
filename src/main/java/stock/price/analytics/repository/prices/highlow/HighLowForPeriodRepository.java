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
                    start_date,
                    wp.high,
                    wp.low,
                    ROW_NUMBER() OVER (PARTITION BY wp.ticker ORDER BY wp.start_date DESC) AS rn
                FROM weekly_prices wp
                WHERE
                    wp.start_date >= CAST(:tradingDate AS DATE) - INTERVAL '75 weeks'  -- Adjust to get the last 75 weeks (buffer zone for inactive tickers between periods)
            	and wp.ticker in (select ticker from stocks where xtb_stock = true and delisted_date is null)
            ),
            filtered_prices AS (
                SELECT
                    lp.ticker,
                    lp.start_date,
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

}
