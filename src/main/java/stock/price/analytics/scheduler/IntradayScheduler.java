package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.CacheService;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.FairValueGapService;
import stock.price.analytics.service.PriceGapService;

@Component
@RequiredArgsConstructor
public class IntradayScheduler {

    private final FairValueGapService fairValueGapService;
    private final CacheService cacheService;
    private final DesktopNotificationService desktopNotificationService;
    private final PriceGapService priceGapService;

    // 10 15,35,55 9-16 * * MON-FRI
    @Scheduled(cron = "${cron.intraday.gaps.update}", zone = "${cron.timezone}")
    public void updateGapsIntraday() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
        priceGapService.closePriceGaps();
    }

    @Schedules({
        @Scheduled(cron = "${cron.intraday.ticker.spikes.between10and16}", zone = "${cron.timezone}"), // 20 15,35,55 10-15 * * MON-FRI
        @Scheduled(cron = "${cron.intraday.ticker.spikes.between16and17}", zone = "${cron.timezone}")  // 20 15,35 16 * * MON-FRI
    })
    public void alertIntradayPriceSpikes() {
        cacheService.tickersByPriceMilestones()
                .forEach((priceMilestone, tickers) -> desktopNotificationService.broadcastDesktopNotification(priceMilestone.toString(), tickers.toString()));
    }

}