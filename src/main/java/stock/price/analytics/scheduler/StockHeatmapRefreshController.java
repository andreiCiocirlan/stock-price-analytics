package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/stock-performance-heatmap")
@RestController
@RequiredArgsConstructor
public class StockHeatmapRefreshController {

    private volatile boolean dataUpdated = false;

    // executed 10 seconds after the Yahoo-Quotes API is called (same cron otherwise)
    @Schedules({
            @Scheduled(cron = "${cron.stocks.heatmap.refresh.intraday.at935}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.stocks.heatmap.refresh.intraday.between10and16}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.stocks.heatmap.refresh.intraday.between16and17}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.stocks.heatmap.refresh.pre.market.between8and9}", zone = "${cron.timezone}"),
            @Scheduled(cron = "${cron.stocks.heatmap.refresh.pre.market.between9and915}", zone = "${cron.timezone}")
    })
    public void updateDataFlag() {
        dataUpdated = true;
    }

    @GetMapping("/refresh-ui")
    public ResponseEntity<Boolean> checkForUpdates() {
        boolean updated = dataUpdated;
        dataUpdated = false; // Reset flag after checking
        return ResponseEntity.ok(updated);
    }

}