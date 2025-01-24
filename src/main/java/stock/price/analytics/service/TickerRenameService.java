package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import stock.price.analytics.repository.prices.TickerRenameRepository;

@Service
@RequiredArgsConstructor
public class TickerRenameService {

    private final TickerRenameRepository tickerRenameRepository;
    private final RefreshMaterializedViewsService refreshMaterializedViewsService;

    public void renameTicker(String oldTicker, String newTicker) {
        tickerRenameRepository.updateStockTicker(oldTicker, newTicker);
        tickerRenameRepository.updateDailyPricesTicker(oldTicker, newTicker);
        tickerRenameRepository.updateWeeklyPricesTicker(oldTicker, newTicker);
        tickerRenameRepository.updateMonthlyPricesTicker(oldTicker, newTicker);
        tickerRenameRepository.updateQuarterlyPricesTicker(oldTicker, newTicker);
        tickerRenameRepository.updateYearlyPricesTicker(oldTicker, newTicker);
        tickerRenameRepository.updateDailyPricesJSONTicker(oldTicker, newTicker);
        tickerRenameRepository.updateHighLow4wTicker(oldTicker, newTicker);
        tickerRenameRepository.updateHighLow52WeekTicker(oldTicker, newTicker);
        tickerRenameRepository.updateHighestLowestPricesTicker(oldTicker, newTicker);
        tickerRenameRepository.updateFairValueGapTicker(oldTicker, newTicker);

        // final step refresh views to reflect changes
        refreshMaterializedViewsService.refreshMaterializedViews();
    }
}