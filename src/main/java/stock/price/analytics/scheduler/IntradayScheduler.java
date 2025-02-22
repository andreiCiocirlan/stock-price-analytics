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

import static stock.price.analytics.model.prices.enums.FvgType.BEARISH;
import static stock.price.analytics.model.prices.enums.FvgType.BULLISH;
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
                    String fvgLabel = fvgLabelFrom(priceMilestone, fvgType, timeframe);
                    Set<String> fvgTaggedTickers = fvgTaggedService.findTickersFVGsTaggedFor(timeframe, fvgType, priceMilestone, cfdMargins54);
                    desktopNotificationService.broadcastDesktopNotification(fvgLabel + fvgTaggedTickers);
                }
            }
        }
    }

    public String fvgLabelFrom(PricePerformanceMilestone pricePerformanceMilestone, FvgType fvgType, StockTimeframe stockTimeframe) {
        String highLowTimeframeCorrelation = timeframeFrom(pricePerformanceMilestone);
        boolean isLow95thPercentile = low95thPercentileValues().contains(pricePerformanceMilestone);
        boolean isHigh95thPercentile = high95thPercentileValues().contains(pricePerformanceMilestone);
        if (isLow95thPercentile && fvgType == BEARISH) {
            // price near lower 95th percentile AND Bearish FVG
            return String.join(" ", stockTimeframe.name(), "ANVIL", highLowTimeframeCorrelation, "FVGs:");
        } else if (isHigh95thPercentile && fvgType == BULLISH) {
            // price near upper 95th percentile AND Bullish FVG
            return String.join(" ", stockTimeframe.name(), "ROCKET SHIP", highLowTimeframeCorrelation, "FVGs:");
        }

        String highLowLabel = isLow95thPercentile ? "Low" : "High";
        String mainFvgLabel = switch (pricePerformanceMilestone) {
            case HIGH_52W_95, LOW_52W_95 -> FVG_95TH_PERCENTILE_52W;
            case HIGH_4W_95, LOW_4W_95 -> FVG_95TH_PERCENTILE_4W;
            case HIGH_ALL_TIME_95, LOW_ALL_TIME_95 -> FVG_95TH_PERCENTILE_ALL_TIME;
            case NEW_ALL_TIME_HIGH, NEW_52W_HIGH, NEW_4W_HIGH, NEW_52W_LOW, NEW_4W_LOW, NEW_ALL_TIME_LOW, NONE ->
                    throw new IllegalStateException("Unexpected value " + pricePerformanceMilestone.name());
        };
        return String.join(" ", stockTimeframe.name(), fvgType.name(), mainFvgLabel, highLowLabel, "FVGs:");
    }

}