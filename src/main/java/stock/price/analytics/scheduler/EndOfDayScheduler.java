package stock.price.analytics.scheduler;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.FairValueGapService;

@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final FairValueGapService fairValueGapService;

    // At the end of the trading day adjust FVGs (INSERT new FVGs found, CLOSE FVGs if no longer OPEN)
    @Scheduled(cron = "${cron.post.market.fvg}", zone = "${cron.timezone}")
    public void updateFVGsAtEOD() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosed();
    }

}