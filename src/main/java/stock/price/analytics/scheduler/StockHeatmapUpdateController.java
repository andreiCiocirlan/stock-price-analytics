package stock.price.analytics.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/stock-heatmap")
@RestController
@RequiredArgsConstructor
public class StockHeatmapUpdateController {

    private volatile boolean dataUpdated = false;

    // same cron timings as YahooPricesScheduler, but 10 seconds later to account for request finishing
    @Scheduled(cron = "10 25,55 8-16 * * MON-FRI", zone = "America/New_York")
    public void updateDataFlag() {
        dataUpdated = true;
    }

    @GetMapping("/refresh")
    public ResponseEntity<Boolean> checkForUpdates() {
        boolean updated = dataUpdated;
        dataUpdated = false; // Reset flag after checking
        return ResponseEntity.ok(updated);
    }

}