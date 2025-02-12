package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.FVGTaggedService;
import stock.price.analytics.service.FairValueGapService;

import java.util.List;

import static stock.price.analytics.model.prices.enums.PricePerformanceMilestone.*;
import static stock.price.analytics.util.Constants.*;

@Component
@RequiredArgsConstructor
public class IntradayScheduler {

    private final FairValueGapService fairValueGapService;
    private final FVGTaggedService fvgTaggedService;
    private final DesktopNotificationService desktopNotificationService;

    @Scheduled(cron = "${cron.intraday.fvg.update}", zone = "${cron.timezone}")
    public void updateFVGsAtIntraday() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosed();
    }

    @Scheduled(cron = "${cron.intraday.fvg.tagged.95th.percentile.4w}", zone = "${cron.timezone}")
    public void alertFVGsTagged95thPercentile4w() {
        alertFVGsTaggedBy(List.of(HIGH_4W_95, LOW_4W_95));
    }

    @Scheduled(cron = "${cron.intraday.fvg.tagged.95th.percentile.52w}", zone = "${cron.timezone}")
    public void alertFVGsTagged95thPercentile52w() {
        alertFVGsTaggedBy(List.of(HIGH_52W_95, LOW_52W_95));
    }

    @Scheduled(cron = "${cron.intraday.fvg.tagged.95th.percentile.ath}", zone = "${cron.timezone}")
    public void alertFVGsTagged95thPercentileATH() {
        alertFVGsTaggedBy(List.of(HIGH_ALL_TIME_95, LOW_ALL_TIME_95));
    }

    private void alertFVGsTaggedBy(List<PricePerformanceMilestone> pricePerformanceMilestones) {
        for (PricePerformanceMilestone priceMilestone : pricePerformanceMilestones) {
            desktopNotificationService.broadcastDesktopNotification(alertPrefixFrom(priceMilestone) + fvgTaggedService.findWeeklyTaggedFVGsBy(priceMilestone, CFD_MARGINS_5X_4X));
        }
    }

    public String alertPrefixFrom(PricePerformanceMilestone pricePerformanceMilestone) {
        return switch (pricePerformanceMilestone) {
            case NEW_ALL_TIME_HIGH, NEW_52W_HIGH, NEW_4W_HIGH, NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW, NONE ->
                    throw new IllegalStateException("Unexpected value " + pricePerformanceMilestone.name());
            case HIGH_52W_95 -> BEARISH_FVG_95TH_PERCENTILE_52W;
            case HIGH_4W_95 -> BEARISH_FVG_95TH_PERCENTILE_4W;
            case HIGH_ALL_TIME_95 -> BEARISH_FVG_95TH_PERCENTILE_ATH;
            case LOW_52W_95 -> BULLISH_FVG_95TH_PERCENTILE_52W;
            case LOW_4W_95 -> BULLISH_FVG_95TH_PERCENTILE_4W;
            case LOW_ALL_TIME_95 -> BULLISH_FVG_95TH_PERCENTILE_ATH;
        };
    }

}