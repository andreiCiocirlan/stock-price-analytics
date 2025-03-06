package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.DiscrepanciesService;

@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final DiscrepanciesService discrepanciesService;
    private final DesktopNotificationService desktopNotificationService;

    // At the end of the trading day check if any stock discrepancies are found
    @Scheduled(cron = "${cron.post.market.checks}", zone = "${cron.timezone}")
    public void findAllStockDiscrepanciesAtEOD() {
        if (!discrepanciesService.findStocksOpeningPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Stocks Opening Price Discrepancies found, check logs!");
        }
        if (!discrepanciesService.findStocksHighLowsOrHTFDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Stocks High-Low/HTF Discrepancies found, check logs!");
        }
        if (!discrepanciesService.findFvgDateDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("FVG Date discrepancies found, check logs!");
        }
        if (!discrepanciesService.findWeeklyOpeningPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Weekly opening price discrepancies found, check logs!");
        }
        if (!discrepanciesService.findWeeklyHighLowPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Weekly High-Low price discrepancies found, check logs!");
        }
    }

}