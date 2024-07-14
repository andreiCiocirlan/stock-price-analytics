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
        refreshMaterializedViewsRepository.refreshDailyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshWeeklyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshMonthlyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshYearlyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshHighLow4w();
        refreshMaterializedViewsRepository.refreshHighLow52w();
    }

}
