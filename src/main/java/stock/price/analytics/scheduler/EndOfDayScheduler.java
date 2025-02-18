package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.FairValueGapService;
import stock.price.analytics.service.PricesService;
import stock.price.analytics.service.StockDiscrepanciesService;

@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final FairValueGapService fairValueGapService;
    private final StockDiscrepanciesService stockDiscrepanciesService;
    private final PricesService pricesService;
    private final DesktopNotificationService desktopNotificationService;

    // At the end of the trading day check if any stock discrepancies are found
    @Scheduled(cron = "${cron.post.market.fvg}", zone = "${cron.timezone}")
    public void findAllStockDiscrepanciesAtEOD() {
        if (!stockDiscrepanciesService.findAllStockDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Stock Discrepancies found, check logs!");
        }
        if (!fairValueGapService.findFvgDateDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("FVG Date discrepancies found, check logs!");
        }
        if (!pricesService.findWeeklyOpeningPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Weekly opening price discrepancies found, check logs!");
        }
        if (!pricesService.findWeeklyHighLowPriceDiscrepancies().isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification("Weekly High-Low price discrepancies found, check logs!");
        }
    }

}