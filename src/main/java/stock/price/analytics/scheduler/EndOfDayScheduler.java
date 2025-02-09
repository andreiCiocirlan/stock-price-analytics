package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.FairValueGapService;

@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final FairValueGapService fairValueGapService;

    // At the end of the trading day adjust FVGs (INSERT new FVGs found, CLOSE FVGs if no longer OPEN)
    @Scheduled(cron = "${cron.expression.post.market.fvg}", zone = "${cron.expression.timezone}")
    public void updateFVGsAtEOD() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            fairValueGapService.findNewFVGsAndSaveFor(timeframe);
            fairValueGapService.updateFVGsHighLowAndClosedFor(timeframe);
        }
    }

}