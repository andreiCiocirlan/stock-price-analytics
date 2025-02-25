package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.FairValueGapService;

@Component
@RequiredArgsConstructor
public class IntradayScheduler {

    private final FairValueGapService fairValueGapService;

    @Scheduled(cron = "${cron.intraday.fvg.update}", zone = "${cron.timezone}")
    public void updateFVGsAtIntraday() {
        fairValueGapService.saveNewFVGsAndUpdateHighLowAndClosedAllTimeframes();
    }

}