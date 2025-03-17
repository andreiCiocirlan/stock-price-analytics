package stock.price.analytics.repository.prices.gaps;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.gaps.PriceGap;

import java.util.List;

@Repository
public interface PriceGapsRepository extends JpaRepository<PriceGap, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            WITH max_date_cte AS (
                select date_trunc('DAY', (select max(last_updated) from stocks)) - INTERVAL '1 DAY' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY date DESC) AS row_num
                FROM daily_prices
                WHERE ticker in (:tickers)
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33))
                    AND date between CURRENT_DATE - INTERVAL '1000 DAYS' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= 1000 and NOT EXISTS (
                    SELECT 1
                    FROM daily_prices p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.date > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'DAILY',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """, nativeQuery = true)
    void saveDailyPriceGaps(List<String> tickers);

    @Modifying
    @Transactional
    @Query(value = """
            WITH max_date_cte AS (
                select date_trunc('WEEK', (select max(last_updated) from stocks)) - INTERVAL '1 WEEK' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    start_date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS row_num
                FROM weekly_prices
                WHERE ticker in (:tickers)
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33))
                    AND start_date between CURRENT_DATE - INTERVAL '300 WEEKS' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= 300 and NOT EXISTS (
                    SELECT 1
                    FROM weekly_prices p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.start_date > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'WEEKLY',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """, nativeQuery = true)
    void saveWeeklyPriceGaps(List<String> tickers);

    @Modifying
    @Transactional
    @Query(value = """
            WITH max_date_cte AS (
                select date_trunc('MONTH', (select max(last_updated) from stocks)) - INTERVAL '1 MONTH' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    start_date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS row_num
                FROM monthly_prices
                WHERE ticker in (:tickers)
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33))
                    AND start_date between CURRENT_DATE - INTERVAL '200 MONTHS' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= 200 and NOT EXISTS (
                    SELECT 1
                    FROM monthly_prices p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.start_date > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'MONTHLY',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """, nativeQuery = true)
    void saveMonthlyPriceGaps(List<String> tickers);

    @Modifying
    @Transactional
    @Query(value = """
            WITH max_date_cte AS (
                select date_trunc('QUARTER', (select max(last_updated) from stocks)) - INTERVAL '4 MONTHS' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    start_date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS row_num
                FROM quarterly_prices
            	WHERE ticker in (:tickers)
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33))
                    AND start_date between CURRENT_DATE - INTERVAL '100 MONTHS' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= 25 and NOT EXISTS (
                    SELECT 1
                    FROM quarterly_prices p2
                    WHERE p2.ticker = p1.ticker
                      AND p2.start_date > p1.closing_date
                      AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'QUARTERLY',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """, nativeQuery = true)
    void saveQuarterlyPriceGaps(List<String> tickers);

    @Modifying
    @Transactional
    @Query(value = """
            WITH max_date_cte AS (
                select date_trunc('YEAR', (select max(last_updated) from stocks)) - INTERVAL '1 YEAR' as max_date
            ),
            ranked_prices AS (
                SELECT
                    ticker,
                    close AS closing_price,
                    start_date AS closing_date,
                    ROW_NUMBER() OVER (PARTITION BY ticker ORDER BY start_date DESC) AS row_num
                FROM yearly_prices
                WHERE ticker in (:tickers)
            	    AND ticker in (select ticker from stocks where cfd_margin in (0.2, 0.25, 0.33))
                    AND start_date between CURRENT_DATE - INTERVAL '10 YEAR' and (SELECT max_date from max_date_cte)
            ),
            unfilled_gaps AS (
                SELECT
                    p1.ticker,
                    p1.closing_price,
                    p1.closing_date
                FROM ranked_prices p1
                WHERE row_num <= 10 and NOT EXISTS (
                    SELECT 1
                    FROM yearly_prices p2
                    WHERE p2.ticker = p1.ticker
                    AND p2.start_date > p1.closing_date
                    AND p1.closing_price BETWEEN p2.low AND p2.high
                )
            )
            INSERT INTO price_gaps (id, ticker, close, timeframe, status, date)
            SELECT
            	nextval('sequence_prices_gaps') AS id,
            	ticker,
            	closing_price,
            	'YEARLY',
            	'OPEN',
            	closing_date
            FROM unfilled_gaps;
            """, nativeQuery = true)
    void saveYearlyPriceGaps(List<String> tickers);

    @Modifying
    @Transactional
    @Query(value = """
            with closed_gaps as (
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'DAILY' AND pg.close between s.d_low and s.d_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'WEEKLY' AND pg.close between s.w_low and s.w_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'MONTHLY' AND pg.close between s.m_low and s.m_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'QUARTERLY' AND pg.close between s.q_low and s.q_high)
            		union all
            	select pg.id from price_gaps pg
            	join stocks s on s.ticker = pg.ticker
            	where (pg.timeframe = 'YEARLY' AND pg.close between s.y_low and s.y_high)
            )
            UPDATE price_gaps
            set status = 'CLOSED'
            where id in (select id from closed_gaps)
            """, nativeQuery = true)
    void closePriceGaps();
}
