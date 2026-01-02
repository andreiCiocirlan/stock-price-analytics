package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.DemarkService;
import stock.price.analytics.service.FairValueGapService;
import stock.price.analytics.service.PriceGapService;

@Component
@RequiredArgsConstructor
public class IntradayScheduler {

    private final FairValueGapService fairValueGapService;
    private final PriceGapService priceGapService;
    private final DemarkService demarkService;

    // 10 15,35,55 9-16 * * MON-FRI
    @Scheduled(cron = "${cron.intraday.gaps.update}", zone = "${cron.timezone}")
    public void updateGapsIntraday() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
        fairValueGapService.closeFVGsForAllTimeframes();
        priceGapService.closePriceGaps();
    }

    // 20 35 9 * * MON-FRI
    @Scheduled(cron = "${cron.intraday.price.gaps.create}", zone = "${cron.timezone}")
    public void createPriceGapsIntraday() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            priceGapService.savePriceGapsTodayFor(timeframe);
        }
    }

    // 30 35 9 * * MON-FRI
    @Scheduled(cron = "${cron.intraday.demark.counts.create}", zone = "${cron.timezone}")
    public void createDemarkCountsIntraday() {
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            demarkService.demarkForTimeframe(timeframe);
        }
    }

}