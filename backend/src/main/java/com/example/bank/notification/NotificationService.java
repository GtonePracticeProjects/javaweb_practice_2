package com.example.bank.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events: keeps one or more open HTTP responses per member and pushes events into them.
 * (In-memory => single instance only. With several servers use Redis pub/sub or a message broker.)
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = emitters.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable cleanup = () -> list.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        send(memberId, emitter, "connected", Map.of("message", "SSE connected"));   // flushes headers right away
        return emitter;
    }

    public void publish(Long memberId, String eventName, Object payload) {
        List<SseEmitter> list = emitters.get(memberId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            send(memberId, emitter, eventName, payload);
        }
    }

    /** Comment-line keep-alive so proxies do not close idle connections. */
    @Scheduled(fixedDelay = 25_000)
    void heartbeat() {
        emitters.forEach((memberId, list) -> list.forEach(e -> {
            try {
                e.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException | IllegalStateException ex) {
                list.remove(e);
            }
        }));
    }

    private void send(Long memberId, SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE client of member {} is gone", memberId);
            emitters.getOrDefault(memberId, List.of()).remove(emitter);
        }
    }
}
