package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate template;

    public void broadcastNotification(String message) {
        this.template.convertAndSend("/topic/desktop-notifications", message);
    }

}