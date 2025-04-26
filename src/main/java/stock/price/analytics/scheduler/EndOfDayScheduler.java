package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DiscrepanciesService;
import stock.price.analytics.service.PriceGapService;
import stock.price.analytics.service.PriceService;
import stock.price.analytics.service.WebSocketNotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final PriceService priceService;
    private final CacheService cacheService;
    private final DiscrepanciesService discrepanciesService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final PriceGapService priceGapService;

    // 0 45 16 * * MON-FRI
    @Scheduled(cron = "${cron.post.market.processing}", zone = "${cron.timezone}")
    public void postProcessingEndOfDay() {
        log.info("EOD post-processing started");

        Map<String, List<String>> discrepancies = checkDiscrepancies();

        // update opening prices for first import of the week, month, quarter, year
        StockTimeframe.higherTimeframes().stream()
                .filter(priceService::isFirstImportDoneFor)
                .forEach(timeframe -> {
                    if (!discrepancies.get("Weekly Opening Price").isEmpty()) {
                        log.info("Updating {} opening prices for stocks, OHLC tables", timeframe);
                        discrepanciesService.updateHTFOpeningPricesDiscrepancyFor(timeframe);
                        discrepanciesService.updateStocksWithOpeningPriceDiscrepancyFor(timeframe);
                    }
                    priceGapService.savePriceGapsTodayFor(cacheService.getCachedTickers(), timeframe);
                });

        // save daily price gaps at EOD
        priceGapService.savePriceGapsTodayFor(cacheService.getCachedTickers(), StockTimeframe.DAILY);
    }

    private Map<String, List<String>> checkDiscrepancies() {
        Map<String, Supplier<List<String>>> discrepancyChecks = Map.of(
                "Stocks Opening Price", discrepanciesService::findStocksOpeningPriceDiscrepancies,
                "Stocks High-Low/HTF", discrepanciesService::findStocksHighLowsOrHTFDiscrepancies,
                "FVG Date", discrepanciesService::findFvgDateDiscrepancies,
                "Weekly Opening Price", discrepanciesService::findWeeklyOpeningPriceDiscrepancies,
                "Weekly High-Low Price", discrepanciesService::findWeeklyHighLowPriceDiscrepancies
        );

        Map<String, List<String>> discrepanciesMap = new HashMap<>();
        discrepancyChecks.forEach((discrepancyType, supplier) -> {
            List<String> discrepancies = supplier.get();
            discrepanciesMap.put(discrepancyType, discrepancies);
            if (!discrepancies.isEmpty()) {
                webSocketNotificationService.broadcastDesktopNotification(
                        "Discrepancy Found",
                        discrepancyType + " Discrepancies found, check logs! Count: " + discrepancies.size()
                );
            }
        });

        return discrepanciesMap;
    }

}