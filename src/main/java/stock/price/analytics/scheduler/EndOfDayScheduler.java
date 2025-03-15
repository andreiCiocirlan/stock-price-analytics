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

    // 0 45 16 * * MON-FRI
    @Scheduled(cron = "${cron.post.market.discrepancy.checks}", zone = "${cron.timezone}")
    public void findAllStockDiscrepanciesAtEOD() {
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
    }

}