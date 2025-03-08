package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.DesktopNotificationService;

@Slf4j
@RequestMapping("/desktop-notification")
@RestController
@RequiredArgsConstructor
public class DesktopNotificationsController {

    private final DesktopNotificationService desktopNotificationService;

    @PostMapping("/send")
    public void sendDesktopNotification(@RequestParam String title, @RequestParam String message) {
        desktopNotificationService.broadcastDesktopNotification(title, message);
        log.info("Desktop notification {} sent with message: {}", title, message);
    }

}