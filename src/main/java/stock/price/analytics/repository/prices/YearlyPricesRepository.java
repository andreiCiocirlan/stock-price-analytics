package stock.price.analytics.repository.prices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stock.price.analytics.model.prices.ohlc.YearlyPrice;

import java.time.LocalDate;
import java.util.List;

public interface YearlyPricesRepository extends JpaRepository<YearlyPrice, Long> {

    @Query(value = """
                SELECT *
                FROM yearly_prices
                WHERE start_date BETWEEN (DATE_TRUNC('year', CURRENT_DATE) - INTERVAL '2 year') AND CURRENT_DATE
                AND ticker in (:tickers)
                ORDER BY ticker, start_date DESC
            """, nativeQuery = true)
    List<YearlyPrice> findPreviousThreeYearlyPricesForTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT y FROM YearlyPrice y WHERE y.ticker = :ticker AND y.startDate < :date")
    List<YearlyPrice> findYearlyByTickerAndStartDateBefore(String ticker, LocalDate date);

    @Query("SELECT y FROM YearlyPrice y WHERE y.ticker = :ticker AND y.startDate = :date")
    List<YearlyPrice> findYearlyByTickerAndStartDate(String ticker, LocalDate date);

}
