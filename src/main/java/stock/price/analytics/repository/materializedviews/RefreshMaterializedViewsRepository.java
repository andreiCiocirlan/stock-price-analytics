package stock.price.analytics.repository.materializedviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;

@Repository
public interface RefreshMaterializedViewsRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW daily_prices_performance_view", nativeQuery = true)
    void refreshDailyPerformanceHeatmapPrices();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW weekly_prices_performance_view", nativeQuery = true)
    void refreshWeeklyPerformanceHeatmapPrices();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW monthly_prices_performance_view", nativeQuery = true)
    void refreshMonthlyPerformanceHeatmapPrices();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW yearly_prices_performance_view", nativeQuery = true)
    void refreshYearlyPerformanceHeatmapPrices();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW latest_prices_view", nativeQuery = true)
    void refreshLatestPrices();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW prev_three_weeks", nativeQuery = true)
    void refreshPrevThreeWeeks();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW prev_two_months", nativeQuery = true)
    void refreshPrevTwoMonths();

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW prev_two_years", nativeQuery = true)
    void refreshPrevTwoYears();

}