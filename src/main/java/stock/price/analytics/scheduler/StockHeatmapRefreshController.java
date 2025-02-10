package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/stock-performance-heatmap")
@RestController
@RequiredArgsConstructor
public class StockHeatmapRefreshController {

    private volatile boolean dataUpdated = false;

    @Scheduled(cron = "${cron.expression.stocks.heatmap.refresh.intraday.at935}", zone = "${cron.expression.timezone}")
    public void updateDataFlagIntradayAt935() {
        dataUpdated = true;
    }

    @Scheduled(cron = "${cron.expression.stocks.heatmap.refresh.intraday.between10and17}", zone = "${cron.expression.timezone}")
    public void updateDataFlagIntradayBetween10and17() {
        dataUpdated = true;
    }

    @GetMapping("/refresh-ui")
    public ResponseEntity<Boolean> checkForUpdates() {
        boolean updated = dataUpdated;
        dataUpdated = false; // Reset flag after checking
        return ResponseEntity.ok(updated);
    }

}