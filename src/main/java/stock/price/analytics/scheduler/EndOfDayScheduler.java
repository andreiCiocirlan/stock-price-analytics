package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.DiscrepanciesService;
import stock.price.analytics.service.PriceGapsService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final CacheService cacheService;
    private final DiscrepanciesService discrepanciesService;
    private final DesktopNotificationService desktopNotificationService;
    private final PriceGapsService priceGapsService;

    // 0 45 16 * * MON-FRI
    @Scheduled(cron = "${cron.post.market.processing}", zone = "${cron.timezone}")
    public void postProcessingEndOfDay() {
        String title = "Discrepancy Found";
        if (!discrepanciesService.findStocksOpeningPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(title, "Stocks Opening Price Discrepancies found, check logs!");
        }
        if (!discrepanciesService.findStocksHighLowsOrHTFDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(title, "Stocks High-Low/HTF Discrepancies found, check logs!");
        }
        if (!discrepanciesService.findFvgDateDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(title, "FVG Date discrepancies found, check logs!");
        }
        if (!discrepanciesService.findWeeklyOpeningPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(title, "Weekly opening price discrepancies found, check logs!");
        }
        if (!discrepanciesService.findWeeklyHighLowPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(title, "Weekly High-Low price discrepancies found, check logs!");
        }

        // update opening prices for first import of the week, month, quarter, year
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            if (cacheService.isFirstImportFor(timeframe)) {
                log.warn("Updating {} opening prices for stocks, OHLC tables", timeframe);
                discrepanciesService.updateHTFOpeningPricesDiscrepancyFor(timeframe);
                discrepanciesService.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
                priceGapsService.savePriceGapsTodayFor(cacheService.getCachedTickers(), timeframe);
            }
        }
        // save daily price gaps at EOD
        priceGapsService.savePriceGapsTodayFor(cacheService.getCachedTickers(), StockTimeframe.DAILY);
    }

}