package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.AbstractPrice;
import stock.price.analytics.model.prices.ohlc.QuarterlyPrice;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface PricesRepository extends JpaRepository<AbstractPrice, Long> {

    @Query(value = """
                SELECT *
                FROM quarterly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '6 month') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<QuarterlyPrice> findPreviousThreeQuarterlyPricesForTickers(List<String> tickers);

    @Query("SELECT q FROM QuarterlyPrice q WHERE q.ticker = :ticker AND q.startDate < :date")
    List<QuarterlyPrice> findQuarterlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT q FROM QuarterlyPrice q WHERE q.ticker = :ticker AND q.startDate = :date")
    List<QuarterlyPrice> findQuarterlyByTickerAndStartDate(String ticker, LocalDate date);

}