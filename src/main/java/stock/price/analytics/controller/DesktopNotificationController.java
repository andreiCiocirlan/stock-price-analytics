package stock.price.analytics.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class DesktopNotificationController {

    @MessageMapping("/app/alert")
    @SendTo("/topic/desktop-notification")
    public String desktopNotify(String message) {
        log.info("Received Desktop Notification: {}", message);
        return message;
    }
}