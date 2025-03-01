package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.PriceMilestoneService;

import static stock.price.analytics.model.prices.enums.PreMarketPriceMilestone.preMarketSchedulerValues;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final PriceMilestoneService priceMilestoneService;
    private final DesktopNotificationService desktopNotificationService;

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.alert.between8and9}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.pre.market.alert.between9and915}", zone = "${cron.timezone}")
    })
    public void alertPreMarketGaps_moreThan_10Percent() {
        priceMilestoneService.findTickersForMilestones(preMarketSchedulerValues(), CFD_MARGINS_5X_4X_3X)
                .forEach((priceMilestone, tickers) -> {
                    if (!tickers.isEmpty())
                        desktopNotificationService.broadcastDesktopNotification(String.join(" ", priceMilestone.toString(), tickers.toString()));
                });
    }

}