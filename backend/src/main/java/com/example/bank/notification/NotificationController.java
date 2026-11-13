package com.example.bank.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Browser: {@code new EventSource('/api/notifications/stream')} (session cookie authenticates). */
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestAttribute("memberId") Long memberId) {
        return notificationService.subscribe(memberId);
    }
}
