package stock.price.analytics.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.materializedviews.RefreshMaterializedViewsRepository;

import static stock.price.analytics.util.LoggingUtil.logTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshMaterializedViewsService {

    private final RefreshMaterializedViewsRepository refreshMaterializedViewsRepository;

    @Transactional
    public void refreshMaterializedViews() {
        refreshLatestAndPerformanceHeatmapPrices();
        refreshDailyJSONPrices();
    }

    private void refreshLatestAndPerformanceHeatmapPrices() {
        logTime(refreshMaterializedViewsRepository::refreshLatestPrices, "refreshed latest prices");
        logTime(refreshMaterializedViewsRepository::refreshDailyPerformanceHeatmapPrices, "refreshed daily performance prices");
        logTime(refreshMaterializedViewsRepository::refreshWeeklyPerformanceHeatmapPrices, "refreshed weekly performance prices");
        logTime(refreshMaterializedViewsRepository::refreshMonthlyPerformanceHeatmapPrices, "refreshed monthly performance prices");
        logTime(refreshMaterializedViewsRepository::refreshYearlyPerformanceHeatmapPrices, "refreshed yearly performance prices");
    }

    public void refreshDailyJSONPrices() {
        logTime(refreshMaterializedViewsRepository::refreshDailyJSONPricesPrices, "refreshed daily prices json");
    }


}