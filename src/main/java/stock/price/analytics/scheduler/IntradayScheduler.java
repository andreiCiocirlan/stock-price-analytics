package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.DailyPricesService;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.FairValueGapService;

import static stock.price.analytics.util.Constants.CFD_MARGINS_5X_4X_3X;

@Component
@RequiredArgsConstructor
public class IntradayScheduler {

    private final FairValueGapService fairValueGapService;
    private final DailyPricesService dailyPricesService;
    private final DesktopNotificationService desktopNotificationService;

    @Scheduled(cron = "${cron.intraday.fvg.update}", zone = "${cron.timezone}")
    public void updateFVGsAtIntraday() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
    }

    @Schedules({
        @Scheduled(cron = "${cron.intraday.ticker.spikes.between10and16}", zone = "${cron.timezone}"),
        @Scheduled(cron = "${cron.intraday.ticker.spikes.between16and17}", zone = "${cron.timezone}")
    })
    public void alertIntradayPriceSpikes() {
        dailyPricesService.tickersWithIntradaySpike(CFD_MARGINS_5X_4X_3X)
                .forEach((priceMilestone, tickers) -> desktopNotificationService.broadcastDesktopNotification(priceMilestone.toString(), tickers.toString()));
    }

}