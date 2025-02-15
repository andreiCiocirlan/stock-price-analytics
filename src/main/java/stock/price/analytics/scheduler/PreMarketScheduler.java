package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.PriceMilestoneService;

import java.util.List;

import static stock.price.analytics.model.prices.enums.PreMarketPriceMilestone.preMarketSchedulerValues;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final PriceMilestoneService priceMilestoneService;
    private final DesktopNotificationService desktopNotificationService;

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.between8and9}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.pre.market.between9and915}", zone = "${cron.timezone}")
    })
    public void alertPreMarketGaps_moreThan_10Percent() {
        priceMilestoneService.findTickersForMilestones(preMarketSchedulerValues(), List.of(0.2, 0.25, 0.33))
                .forEach((priceMilestone, tickers) -> {
                    if (!tickers.isEmpty())
                        desktopNotificationService.broadcastDesktopNotification(String.join(" ", priceMilestone, tickers.toString()));
                });
    }

}