package stock.price.analytics.repository.stocks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.stocks.Stock;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {


    @Modifying
    @Query(value = """
            UPDATE stocks s
            SET
                highest = hlp.high,
                lowest = hlp.low
            FROM (
                SELECT ticker, high, low
                FROM highest_lowest
                WHERE (ticker, start_date) IN (
                    SELECT ticker, MAX(start_date)
                    FROM highest_lowest
                    GROUP BY ticker
                )
            ) hlp
            WHERE s.ticker = hlp.ticker;
            """, nativeQuery = true)
    void updateHighestLowest();

    @Modifying
    @Query(value = """
            UPDATE stocks s
            SET
                high52w = h52w.high52w,
                low52w = h52w.low52w
            FROM (
                SELECT ticker, high AS high52w, low AS low52w
                FROM high_low52w
                WHERE (ticker, start_date) IN (
                    SELECT ticker, MAX(start_date)
                    FROM high_low52w
                    GROUP BY ticker
                )
            ) h52w
            WHERE s.ticker = h52w.ticker;
            """, nativeQuery = true)
    void updateHighLow52w();

    @Modifying
    @Query(value = """
            UPDATE stocks s
            SET
                high4w = h4w.high4w,
                low4w = h4w.low4w
            FROM (
                SELECT ticker, high AS high4w, low AS low4w
                FROM high_low4w
                WHERE (ticker, start_date) IN (
                    SELECT ticker, MAX(start_date)
                    FROM high_low4w
                    GROUP BY ticker
                )
            ) h4w
            WHERE s.ticker = h4w.ticker;
            """, nativeQuery = true)
    void updateHighLow4w();

    @Modifying
    @Query(value = """
            WITH stocksToUpdate AS (
            	SELECT ticker
            	FROM stocks
            	WHERE delisted_date is null and last_updated <> CAST(:tradingDate AS DATE) and ticker in (:tickers)
            )
            UPDATE stocks
            SET	last_updated = CAST(:tradingDate AS DATE)
            WHERE ticker in (select ticker from stocksToUpdate)
            """, nativeQuery = true)
    void updateStocksLastUpdated(LocalDate tradingDate, List<String> tickers);

    @Modifying
    @Query(value = """
            WITH hl_prices AS (
            			SELECT
            				hl52.ticker,
            				hl52.high AS high52w_high,
            				hl52.low AS low52w_low,
            				NULL::double precision AS high4w_high,
            				NULL::double precision AS low4w_low,
            				NULL::double precision AS highest_high,
            				NULL::double precision AS lowest_low
            			FROM high_low52w hl52
            			WHERE start_date = CAST(:tradingDate AS DATE)
            
            			UNION ALL
            
            			SELECT
            				hl4.ticker,
            				NULL::double precision AS high52w_high,
            				NULL::double precision AS low52w_low,
            				hl4.high AS high4w_high,
            				hl4.low AS low4w_low,
            				NULL::double precision AS highest_high,
            				NULL::double precision AS lowest_low
            			FROM high_low4w hl4
            			WHERE start_date = CAST(:tradingDate AS DATE)
            
            			UNION ALL
            
            			SELECT
            				hl.ticker,
            				NULL::double precision AS high52w_high,
            				NULL::double precision AS low52w_low,
            				NULL::double precision AS high4w_high,
            				NULL::double precision AS low4w_low,
            				hl.high AS highest_high,
            				hl.low AS lowest_low
            			FROM highest_lowest hl
            			WHERE start_date = CAST(:tradingDate AS DATE)
            		),
            		merged_result as (
            			SELECT
            				ticker,
            				MAX(highest_high) AS highest,
            				MAX(high52w_high) AS high52w,
            				MAX(high4w_high) AS high4w,
            				MIN(lowest_low) AS lowest,
            				MIN(low52w_low) AS low52w,
            				MIN(low4w_low) AS low4w
            			FROM hl_prices
            			GROUP BY ticker
            		)
                    UPDATE stocks s
            		SET
            			high4w = l.high4w,
            			high52w = l.high52w,
            			highest = l.highest,
            			low4w = l.low4w,
            			low52w = l.low52w,
            			lowest = l.lowest
            		FROM merged_result l
            		WHERE s.ticker = l.ticker;
            """, nativeQuery = true)
    void updateStocksHighLow(LocalDate tradingDate);

    @Modifying
    @Query(value = """
            UPDATE stocks s
            SET last_updated = subquery.latest_date
            FROM (
                SELECT ticker, MAX(date) AS latest_date
                FROM daily_prices
                GROUP BY ticker
            ) subquery
            WHERE s.ticker = subquery.ticker;
            """, nativeQuery = true)
    void saveStockUpdatedDate(); // only for new stocks with history

    List<Stock> findByXtbStockIsTrue();

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET d_open = dp.open, close = dp.close, d_low = dp.low,d_high = dp.high, d_performance = dp.performance
                FROM daily_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.date = (select last_updated FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockDailyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET w_open = dp.open, close = dp.close, w_low = dp.low,w_high = dp.high, w_performance = dp.performance
                FROM weekly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockWeeklyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET m_open = dp.open, close = dp.close, m_low = dp.low,m_high = dp.high, m_performance = dp.performance
                FROM monthly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('MONTH', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockMonthlyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET q_open = dp.open, close = dp.close, q_low = dp.low,q_high = dp.high, q_performance = dp.performance
                FROM quarterly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('QUARTER', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockQuarterlyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET y_open = dp.open, close = dp.close, y_low = dp.low,y_high = dp.high, y_performance = dp.performance
                FROM yearly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('YEAR', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockYearlyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET high52w = dp.high, low52w = dp.low
                FROM high_low52w dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateHighLow52wPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET high4w = dp.high, low4w = dp.low
                FROM high_low4w dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateHighLow4wPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET highest = dp.high, lowest = dp.low
                FROM highest_lowest dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.start_date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateHighestLowestPricesFor(String ticker);
}
