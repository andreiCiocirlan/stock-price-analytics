package stock.price.analytics.repository.materializedviews;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stock.price.analytics.model.prices.ohlc.AbstractPriceOHLC;

@Repository
public interface RefreshMaterializedViewsRepository extends JpaRepository<AbstractPriceOHLC, Long> {

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW daily_prices_performance_view", nativeQuery = true)
    void refreshDailyPerformanceHeatmapPrices();

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW weekly_prices_performance_view", nativeQuery = true)
    void refreshWeeklyPerformanceHeatmapPrices();

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW monthly_prices_performance_view", nativeQuery = true)
    void refreshMonthlyPerformanceHeatmapPrices();

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW yearly_prices_performance_view", nativeQuery = true)
    void refreshYearlyPerformanceHeatmapPrices();

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW high_low4w_view", nativeQuery = true)
    void refreshHighLow4w();

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW high_low52w_view", nativeQuery = true)
    void refreshHighLow52w();

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW latest_prices", nativeQuery = true)
    void refreshLatestPrices();
}