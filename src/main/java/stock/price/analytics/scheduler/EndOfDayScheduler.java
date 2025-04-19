package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DiscrepancieService;
import stock.price.analytics.service.PriceGapService;
import stock.price.analytics.service.PriceService;
import stock.price.analytics.service.WebSocketNotificationService;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final PriceService priceService;
    private final CacheService cacheService;
    private final DiscrepancieService discrepancieService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final PriceGapService priceGapService;

    // 0 45 16 * * MON-FRI
    @Scheduled(cron = "${cron.post.market.processing}", zone = "${cron.timezone}")
    public void postProcessingEndOfDay() {
        log.info("EOD post-processing started");

        Map<String, Supplier<List<String>>> discrepancyChecks = Map.of(
                "Stocks Opening Price", discrepancieService::findStocksOpeningPriceDiscrepancies,
                "Stocks High-Low/HTF", discrepancieService::findStocksHighLowsOrHTFDiscrepancies,
                "FVG Date", discrepancieService::findFvgDateDiscrepancies,
                "Weekly Opening Price", discrepancieService::findWeeklyOpeningPriceDiscrepancies,
                "Weekly High-Low Price", discrepancieService::findWeeklyHighLowPriceDiscrepancies
        );

        discrepancyChecks.forEach((discrepancyType, supplier) -> {
            List<?> discrepancies = supplier.get();
            if (!discrepancies.isEmpty()) {
                webSocketNotificationService.broadcastDesktopNotification(
                        "Discrepancy Found",
                        discrepancyType + " Discrepancies found, check logs! Discrepancies count: " + discrepancies.size()
                );
            }
        });

        // update opening prices for first import of the week, month, quarter, year
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            if (priceService.isFirstImportDoneFor(timeframe)) {
                if (discrepancyChecks.containsKey("Weekly Opening Price")) {
                    log.info("Updating {} opening prices for stocks, OHLC tables", timeframe);
                    discrepancieService.updateHTFOpeningPricesDiscrepancyFor(timeframe);
                    discrepancieService.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
                }
                priceGapService.savePriceGapsTodayFor(cacheService.getCachedTickers(), timeframe);
            }
        }
        // save daily price gaps at EOD
        priceGapService.savePriceGapsTodayFor(cacheService.getCachedTickers(), StockTimeframe.DAILY);
    }

}