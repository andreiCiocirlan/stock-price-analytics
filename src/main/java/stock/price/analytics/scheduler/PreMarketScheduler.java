package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.cache.CacheInitializationService;
import stock.price.analytics.service.HighLowForPeriodService;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final HighLowForPeriodService highLowForPeriodService;

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.alert.between8and915}", zone = "${cron.timezone}") // 10 0,15 9 * * MON-FRI
    })
    public void logNewHighLowsThisWeek() {
        highLowForPeriodService.logNewHighLowsThisWeek();
    }

    private final CacheInitializationService cacheInitializationService;

    // 0 1 15 * * MON-FRI
    @Scheduled(cron = "${cron.pre.market.clear.cache.reinit}", zone = "${cron.timezone}")
    public void clearCachesAndReinitializeEndOfDay() {
        cacheInitializationService.clearCachesAndReinitialize();
    }

}