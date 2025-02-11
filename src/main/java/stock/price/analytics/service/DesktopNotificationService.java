package stock.price.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesktopNotificationService {

    private final SimpMessagingTemplate template;

    public void broadcastDesktopNotification(String message) {
        log.info("{}", message);
        this.template.convertAndSend("/topic/desktop-notification", message);
    }

}