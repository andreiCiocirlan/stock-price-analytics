package stock.price.analytics.repository.stocks;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.stocks.Stock;

@Repository
public interface TickerRenameRepository extends JpaRepository<Stock, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.ticker = :newTicker WHERE s.ticker = :oldTicker")
    void updateStockTicker(@Param("oldTicker") String oldTicker,
                            @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE DailyPrice d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    void updateDailyPricesTicker(@Param("oldTicker") String oldTicker, 
                                 @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE WeeklyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    void updateWeeklyPricesTicker(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE MonthlyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    void updateMonthlyPricesTicker(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE QuarterlyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    void updateQuarterlyPricesTicker(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE YearlyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    void updateYearlyPricesTicker(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE DailyPricesJSON d SET d.symbol = :newTicker WHERE d.symbol = :oldTicker")
    void updateDailyPricesJSONTicker(@Param("oldTicker") String oldTicker,
                                 @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE HighLow4w d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    void updateHighLow4wTicker(@Param("oldTicker") String oldTicker,
                               @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE HighLow52Week d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    void updateHighLow52WeekTicker(@Param("oldTicker") String oldTicker,
                                   @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE HighestLowestPrices d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    void updateHighestLowestPricesTicker(@Param("oldTicker") String oldTicker,
                                         @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE FairValueGap d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    void updateFairValueGapTicker(@Param("oldTicker") String oldTicker,
                                         @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE PriceGap d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    void updatPriceGapTicker(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);
}