package stock.price.analytics.service;

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

    public void refreshMaterializedViews() {
        refreshLatestAndPerformanceHeatmapPrices();
        refreshPreviousHighTimeframePrices();
    }

    private void refreshLatestAndPerformanceHeatmapPrices() {
        logTime(refreshMaterializedViewsRepository::refreshLatestPrices, "refreshed latest prices");
        logTime(refreshMaterializedViewsRepository::refreshDailyPerformanceHeatmapPrices, "refreshed daily performance prices");
        logTime(refreshMaterializedViewsRepository::refreshWeeklyPerformanceHeatmapPrices, "refreshed weekly performance prices");
        logTime(refreshMaterializedViewsRepository::refreshMonthlyPerformanceHeatmapPrices, "refreshed monthly performance prices");
        logTime(refreshMaterializedViewsRepository::refreshYearlyPerformanceHeatmapPrices, "refreshed yearly performance prices");
    }

    private void refreshPreviousHighTimeframePrices() {
        logTime(refreshMaterializedViewsRepository::refreshPrevTwoWeeks, "refreshed prev two weeks prices");
        logTime(refreshMaterializedViewsRepository::refreshPrevTwoMonths, "refreshed prev two months prices");
        logTime(refreshMaterializedViewsRepository::refreshPrevTwoYears, "refreshed prev two years prices");
    }

}
