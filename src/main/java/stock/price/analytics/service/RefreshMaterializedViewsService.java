package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.materializedviews.RefreshMaterializedViewsRepository;

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
        refreshMaterializedViewsRepository.refreshLatestPrices();
        log.info("refreshed latest prices");
        refreshMaterializedViewsRepository.refreshDailyPerformanceHeatmapPrices();
        log.info("refreshed daily performance prices");
        refreshMaterializedViewsRepository.refreshWeeklyPerformanceHeatmapPrices();
        log.info("refreshed weekly performance prices");
        refreshMaterializedViewsRepository.refreshMonthlyPerformanceHeatmapPrices();
        log.info("refreshed monthly performance prices");
        refreshMaterializedViewsRepository.refreshYearlyPerformanceHeatmapPrices();
        log.info("refreshed yearly performance prices");
    }

    private void refreshPreviousHighTimeframePrices() {
        refreshMaterializedViewsRepository.refreshPrevTwoWeeks();
        log.info("refreshed prev two weeks prices");
        refreshMaterializedViewsRepository.refreshPrevTwoMonths();
        log.info("refreshed prev two months prices");
        refreshMaterializedViewsRepository.refreshPrevTwoYears();
        log.info("refreshed prev two years prices");
    }

}
