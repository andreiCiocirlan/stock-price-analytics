package stock.price.analytics.repository.stocks;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRenameRepository extends StockRepository {

    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.ticker = :newTicker WHERE s.ticker = :oldTicker")
    int updateStock(@Param("oldTicker") String oldTicker,
                            @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE DailyPrice d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    int updateDailyPrices(@Param("oldTicker") String oldTicker,
                                 @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE WeeklyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    int updateWeeklyPrices(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE MonthlyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    int updateMonthlyPrices(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE QuarterlyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    int updateQuarterlyPrices(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE YearlyPrice w SET w.ticker = :newTicker WHERE w.ticker = :oldTicker")
    int updateYearlyPrices(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE DailyPricesJSON d SET d.symbol = :newTicker WHERE d.symbol = :oldTicker")
    int updateDailyPricesJSON(@Param("oldTicker") String oldTicker,
                                 @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE HighLow4w d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    int updateHighLow4w(@Param("oldTicker") String oldTicker,
                               @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE HighLow52Week d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    int updateHighLow52Week(@Param("oldTicker") String oldTicker,
                                   @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE HighestLowestPrices d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    int updateHighestLowestPrices(@Param("oldTicker") String oldTicker,
                                         @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE FairValueGap d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    int updateFairValueGap(@Param("oldTicker") String oldTicker,
                                         @Param("newTicker") String newTicker);

    @Modifying
    @Transactional
    @Query("UPDATE PriceGap d SET d.ticker = :newTicker WHERE d.ticker = :oldTicker")
    int updatePriceGap(@Param("oldTicker") String oldTicker,
                                  @Param("newTicker") String newTicker);
}