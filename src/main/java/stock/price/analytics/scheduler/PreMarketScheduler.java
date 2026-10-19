package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import stock.price.analytics.model.prices.enums.StockTimeframe;
import stock.price.analytics.service.HighLowForPeriodService;
import stock.price.analytics.service.PriceService;
import stock.price.analytics.service.StdDevProjectionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreMarketScheduler {

    private final HighLowForPeriodService highLowForPeriodService;
    private final StdDevProjectionService stdDevProjectionService;
    private final PriceService priceService;

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
        // 1. Always run daily
        log.info("Running DAILY projections insert...");
        stdDevProjectionService.insertStdDevProjectionsFor(StockTimeframe.DAILY);

        // 2. Conditionally run higher timeframes if first import of the week, month, quarter, year
        for (StockTimeframe timeframe : StockTimeframe.higherTimeframes()) {
            boolean isAlreadyImported = priceService.isFirstImportDoneFor(timeframe);
            if (!isAlreadyImported) {
                log.info("First import of the period detected for {}. Running projections insert...", timeframe);
                stdDevProjectionService.insertStdDevProjectionsFor(timeframe);
            } else {
                log.debug("Data for {} already exists for the current period. Skipping insert.", timeframe);
            }
        }
    }

}