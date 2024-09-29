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
            UPDATE stocks s
            SET delisted_date = (
              SELECT MAX(dp.date)
              FROM daily_prices dp
              WHERE dp.ticker = s.ticker
              HAVING MAX(dp.date) < CURRENT_DATE - INTERVAL '10 day'
            ), ipo_date = (
              SELECT MIN(dp.date)
              FROM daily_prices dp
              WHERE dp.ticker = s.ticker
            )
            """, nativeQuery = true)
    void updateIpoAndDelistedDates();

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
            UPDATE stocks s
            SET
                high4w = l.high4w,
                high52w = l.high52w,
                highest = l.highest,
                low4w = l.low4w,
                low52w = l.low52w,
                lowest = l.lowest
            FROM latest_high_low l
            WHERE s.ticker = l.ticker;
            """, nativeQuery = true)
    void updateStocksHighLow();

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
}
