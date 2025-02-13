package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.StockDiscrepanciesService;

@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final StockDiscrepanciesService stockDiscrepanciesService;
    private final DesktopNotificationService desktopNotificationService;

    // At the end of the trading day check if any stock discrepancies are found
    @Scheduled(cron = "${cron.post.market.fvg}", zone = "${cron.timezone}")
    public void findAllStockDiscrepanciesAtEOD() {
        if (stockDiscrepanciesService.findAllStockDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Discrepancies found, check logs!");
        }
    }

}