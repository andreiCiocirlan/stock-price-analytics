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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
        log.info("EOD post-processing started");

        Map<String, Supplier<List<String>>> discrepancyChecks = Map.of(
                "Stocks Opening Price", discrepanciesService::findStocksOpeningPriceDiscrepancies,
                "Stocks High-Low/HTF", discrepanciesService::findStocksHighLowsOrHTFDiscrepancies,
                "FVG Date", discrepanciesService::findFvgDateDiscrepancies,
                "Weekly Opening Price", discrepanciesService::findWeeklyOpeningPriceDiscrepancies,
                "Weekly High-Low Price", discrepanciesService::findWeeklyHighLowPriceDiscrepancies
        );

        discrepancyChecks.forEach((discrepancyType, supplier) -> {
            List<?> discrepancies = supplier.get();
            if (!discrepancies.isEmpty()) {
                desktopNotificationService.broadcastDesktopNotification(
                        "Discrepancy Found",
                        discrepancyType + " Discrepancies found, check logs! Discrepancies count: " + discrepancies.size()
                );
            }
        });

        // update opening prices for first import of the week, month, quarter, year
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            if (cacheService.isFirstImportFor(timeframe)) {
                log.info("Updating {} opening prices for stocks, OHLC tables", timeframe);
                discrepanciesService.updateHTFOpeningPricesDiscrepancyFor(timeframe);
                discrepanciesService.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
                priceGapsService.savePriceGapsTodayFor(cacheService.getCachedTickers(), timeframe);
            }
        }
        // save daily price gaps at EOD
        priceGapsService.savePriceGapsTodayFor(cacheService.getCachedTickers(), StockTimeframe.DAILY);
    }

}