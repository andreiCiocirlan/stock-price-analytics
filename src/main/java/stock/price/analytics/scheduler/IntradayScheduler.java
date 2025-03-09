package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.FairValueGapService;
import stock.price.analytics.service.PriceMilestoneService;

import static stock.price.analytics.model.prices.enums.IntradayPriceSpike.intradaySpikes;
import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;

@Component
@RequiredArgsConstructor
public class IntradayScheduler {

    private final FairValueGapService fairValueGapService;
    private final PriceMilestoneService priceMilestoneService;
    private final DesktopNotificationService desktopNotificationService;

    // 10 15,35,55 9-16 * * MON-FRI
    @Scheduled(cron = "${cron.intraday.fvg.update}", zone = "${cron.timezone}")
    public void updateFVGsAtIntraday() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
    }

    @Schedules({
        @Scheduled(cron = "${cron.intraday.ticker.spikes.between10and16}", zone = "${cron.timezone}"), // 20 15,35,55 10-15 * * MON-FRI
        @Scheduled(cron = "${cron.intraday.ticker.spikes.between16and17}", zone = "${cron.timezone}")  // 20 15,35 16 * * MON-FRI
    })
    public void alertIntradayPriceSpikes() {
        priceMilestoneService.findTickersForMilestones(intradaySpikes(), CFD_MARGINS_5X_4X_3X)
                .forEach((priceMilestone, tickers) -> desktopNotificationService.broadcastDesktopNotification(priceMilestone.toString(), tickers.toString()));
    }

}