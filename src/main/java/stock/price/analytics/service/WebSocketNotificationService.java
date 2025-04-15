package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate template;

    public void broadcastDesktopNotification(String title, String message) {
        log.info("{}", message);
        this.template.convertAndSend("/topic/desktop-notification", Map.of("title", title, "message", message));
    }

    public void broadcastStockChartUpdate() {
        log.info("Broadcasting stock chart update signal");
        this.template.convertAndSend("/topic/stock-updates", "update"); // simple signal message
    }

}