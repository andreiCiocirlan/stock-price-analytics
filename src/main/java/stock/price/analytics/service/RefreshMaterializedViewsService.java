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
        refreshMaterializedViewsRepository.refreshLatestPrices();
        log.info("refreshed latest prices");
        refreshMaterializedViewsRepository.refreshDailyPerformanceHeatmapPrices();
        log.info("refreshed heatmap prices");
        refreshMaterializedViewsRepository.refreshWeeklyPerformanceHeatmapPrices();
        log.info("refreshed weekly performance prices");
        refreshMaterializedViewsRepository.refreshMonthlyPerformanceHeatmapPrices();
        log.info("refreshed monthly performance prices");
        refreshMaterializedViewsRepository.refreshYearlyPerformanceHeatmapPrices();
        log.info("refreshed yearly performance prices");
        refreshMaterializedViewsRepository.refreshHighLow4w();
        log.info("refreshed high low 4w");
        refreshMaterializedViewsRepository.refreshHighLow52w();
        log.info("refreshed high low 52w");
    }

}
