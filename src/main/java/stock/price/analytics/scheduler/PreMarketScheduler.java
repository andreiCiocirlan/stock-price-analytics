package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.HighLowForPeriodService;
import stock.price.analytics.service.PriceMilestoneService;
import stock.price.analytics.service.WebSocketNotificationService;
import stock.price.analytics.util.PriceMilestoneFactory;

import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final HighLowForPeriodService highLowForPeriodService;
    private final PriceMilestoneService priceMilestoneService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.alert.between8and9}", zone = "${cron.timezone}"),  // 10 15,30,45 8 * * MON-FRI
            @Scheduled(cron = "${cron.pre.market.alert.between9and915}", zone = "${cron.timezone}") // 10 0,15 9 * * MON-FRI
    })
    public void alertPreMarketGaps_moreThan_10Percent() {
        priceMilestoneService.findTickersForMilestones(PriceMilestoneFactory.preMarketSchedulerValues(), CFD_MARGINS_5X_4X_3X)
                .forEach((priceMilestone, tickers) -> webSocketNotificationService.broadcastDesktopNotification("Pre-Market alert", String.join(" ", priceMilestone.toString(), tickers.toString())));
        highLowForPeriodService.logNewHighLowsThisWeek();
    }

}