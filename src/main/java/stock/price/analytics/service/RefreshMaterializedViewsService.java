package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.materializedviews.RefreshMaterializedViewsRepository;

import static stock.price.analytics.util.LoggingUtil.logElapsedTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshMaterializedViewsService {

    private final RefreshMaterializedViewsRepository refreshMaterializedViewsRepository;

    @Transactional
    public void refreshMaterializedViews() {
        refreshLatestAndPerformanceHeatmapPrices();
    }

    private void refreshLatestAndPerformanceHeatmapPrices() {
        logElapsedTime(refreshMaterializedViewsRepository::refreshLatestPrices, "refreshed latest prices");
        logElapsedTime(refreshMaterializedViewsRepository::refreshDailyPerformanceHeatmapPrices, "refreshed daily performance prices");
        logElapsedTime(refreshMaterializedViewsRepository::refreshWeeklyPerformanceHeatmapPrices, "refreshed weekly performance prices");
        logElapsedTime(refreshMaterializedViewsRepository::refreshMonthlyPerformanceHeatmapPrices, "refreshed monthly performance prices");
        logElapsedTime(refreshMaterializedViewsRepository::refreshYearlyPerformanceHeatmapPrices, "refreshed yearly performance prices");
    }

}