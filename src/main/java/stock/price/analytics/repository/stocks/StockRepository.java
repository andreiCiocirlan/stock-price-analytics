package stock.price.analytics.repository.stocks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.stocks.Stock;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {


    List<Stock> findByXtbStockIsTrueAndDelistedDateIsNull();

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
                  AND dp.date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockWeeklyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET m_open = dp.open, close = dp.close, m_low = dp.low,m_high = dp.high, m_performance = dp.performance
                FROM monthly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.date = (SELECT date_trunc('MONTH', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockMonthlyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET q_open = dp.open, close = dp.close, q_low = dp.low,q_high = dp.high, q_performance = dp.performance
                FROM quarterly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.date = (SELECT date_trunc('QUARTER', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockQuarterlyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET y_open = dp.open, close = dp.close, y_low = dp.low,y_high = dp.high, y_performance = dp.performance
                FROM yearly_prices dp
                WHERE dp.ticker = stocks.ticker
                  AND dp.ticker = :ticker
                  AND dp.date = (SELECT date_trunc('YEAR', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateStockYearlyPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET high52w = hl.high, low52w = hl.low
                FROM high_low52w hl
                WHERE hl.ticker = stocks.ticker
                  AND hl.ticker = :ticker
                  AND hl.date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateHighLow52wPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET high4w = hl.high, low4w = hl.low
                FROM high_low4w hl
                WHERE hl.ticker = stocks.ticker
                  AND hl.ticker = :ticker
                  AND hl.date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateHighLow4wPricesFor(String ticker);

    @Modifying
    @Query(value = """
            UPDATE stocks
                SET highest = hl.high, lowest = hl.low
                FROM highest_lowest hl
                WHERE hl.ticker = stocks.ticker
                  AND hl.ticker = :ticker
                  AND hl.date = (SELECT date_trunc('WEEK', last_updated) FROM stocks WHERE ticker = :ticker)
            """, nativeQuery = true)
    void updateHighestLowestPricesFor(String ticker);
}
