package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.FvgType;
import stock.price.analytics.model.prices.enums.PricePerformanceMilestone;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DesktopNotificationService;
import stock.price.analytics.service.FVGTaggedService;
import stock.price.analytics.service.FairValueGapService;

import java.util.Set;
import java.util.stream.Collectors;

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
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
    }

    @Scheduled(cron = "${cron.intraday.fvg.tagged.95th.percentile}", zone = "${cron.timezone}")
    public void alertFVGsTagged95thPercentile() {
        String cfdMargins54 = CFD_MARGINS_5X_4X.stream().map(cfdMargin -> STR."'\{cfdMargin}'").collect(Collectors.joining(", "));
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            for (PricePerformanceMilestone priceMilestone : milestones95thPercentile()) {
                for (FvgType fvgType : FvgType.values()) {
                    String fvgLabel = fairValueGapService.fvgLabelFrom(priceMilestone, fvgType, timeframe);
                    Set<String> fvgTaggedTickers = fvgTaggedService.findTickersFVGsTaggedFor(timeframe, fvgType, priceMilestone, cfdMargins54);
                    desktopNotificationService.broadcastDesktopNotification(fvgLabel + fvgTaggedTickers);
                }
            }
        }
    }

}