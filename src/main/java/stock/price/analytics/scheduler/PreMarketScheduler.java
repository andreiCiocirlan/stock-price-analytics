package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.service.HighLowForPeriodService;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final HighLowForPeriodService highLowForPeriodService;

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.alert.between8and9}", zone = "${cron.timezone}"),  // 10 15,30,45 8 * * MON-FRI
            @Scheduled(cron = "${cron.pre.market.alert.between9and915}", zone = "${cron.timezone}") // 10 0,15 9 * * MON-FRI
    })
    public void alertPreMarketGaps_moreThan_10Percent() {
        highLowForPeriodService.logNewHighLowsThisWeek();
    }

}