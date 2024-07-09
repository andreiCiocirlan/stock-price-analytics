package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.repository.materializedviews.RefreshMaterializedViewsRepository;

@RestController
@RequestMapping("/refresh")
@RequiredArgsConstructor
public class RefreshMaterializedViewsController {

    private final RefreshMaterializedViewsRepository refreshMaterializedViewsRepository;

    @GetMapping("/views")
    public void refreshMaterializedViews() {
        refreshMaterializedViewsRepository.refreshWeeklyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshMonthlyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshYearlyPerformanceHeatmapPrices();
        refreshMaterializedViewsRepository.refreshHighLow4w();
        refreshMaterializedViewsRepository.refreshHighLow52w();
    }


}
