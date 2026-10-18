package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.HighLowForPeriodService;
import stock.price.analytics.service.StdDevProjectionService;

@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final HighLowForPeriodService highLowForPeriodService;
    private final StdDevProjectionService stdDevProjectionService;

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.alert.between8and915}", zone = "${cron.timezone}") // 10 0,15 9 * * MON-FRI
    })
    public void logNewHighLowsThisWeek() {
        highLowForPeriodService.logNewHighLowsThisWeek();
    }

    @Schedules({
            @Scheduled(cron = "${cron.pre.market.insert.stddev.projections}", zone = "${cron.timezone}") // 0 25 9 * * MON-FRI
    })
    public void insertStdDevProjections() {
        for (StockTimeframe timeframe : StockTimeframe.values()) {
            stdDevProjectionService.insertStdDevProjectionsFor(timeframe);
        }
    }

}