package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.PriceMilestoneService;

import java.util.List;

import static stock.price.analytics.model.prices.enums.PreMarketPriceMilestone.GAP_DOWN_10_PERCENT;
import static stock.price.analytics.model.prices.enums.PreMarketPriceMilestone.GAP_UP_10_PERCENT;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final PriceMilestoneService priceMilestoneService;
    private final DesktopNotificationService desktopNotificationService;

    @Scheduled(cron = "${cron.expression.pre.market}", zone = "${cron.expression.timezone}")
    public void alertPreMarketGaps_moreThan_10Percent() {
        List<String> preMarketTickersGapUp10Percent = priceMilestoneService.findTickersForMilestone(GAP_UP_10_PERCENT.name(), List.of(0.2, 0.25, 0.33));
        List<String> preMarketTickersGapDown10Percent = priceMilestoneService.findTickersForMilestone(GAP_DOWN_10_PERCENT.name(), List.of(0.2, 0.25, 0.33));
        if (!preMarketTickersGapUp10Percent.isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(String.join(" ", GAP_UP_10_PERCENT.toString(), preMarketTickersGapUp10Percent.toString()));
        }
        if (!preMarketTickersGapDown10Percent.isEmpty()) {
            desktopNotificationService.broadcastDesktopNotification(String.join(" ", GAP_DOWN_10_PERCENT.toString(), preMarketTickersGapDown10Percent.toString()));
        }
    }
}