package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
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

    @Schedules({
            @Scheduled(cron = "${cron.expression.pre.market.between8and9}", zone = "${cron.expression.timezone}"),
            @Scheduled(cron = "${cron.expression.pre.market.between9and915}", zone = "${cron.expression.timezone}")
    })
    public void alertPreMarketGaps_moreThan_10Percent() {
        priceMilestoneService.findTickersForMilestones(List.of(GAP_UP_10_PERCENT, GAP_DOWN_10_PERCENT), List.of(0.2, 0.25, 0.33))
                .forEach((priceMilestone, tickers) -> {
                    if (!tickers.isEmpty())
                        desktopNotificationService.broadcastDesktopNotification(String.join(" ", priceMilestone, tickers.toString()));
                });
    }

}