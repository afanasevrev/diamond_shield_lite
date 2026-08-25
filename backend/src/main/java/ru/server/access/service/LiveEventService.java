package ru.server.access.service;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LiveEventService {

    private static final Logger log = LoggerFactory.getLogger(LiveEventService.class);

    private final List<SseClient> clients = new CopyOnWriteArrayList<>();

    private final Gson gson;

    public LiveEventService(Gson gson) {
        this.gson = gson;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        SseClient client = new SseClient(emitter);

        clients.add(client);

        emitter.onCompletion(() -> {
            client.markDead();
            clients.remove(client);
        });

        emitter.onTimeout(() -> {
            client.markDead();
            clients.remove(client);
        });

        emitter.onError(error -> {
            client.markDead();
            clients.remove(client);
        });

        if (!client.send("connected", "{\"connected\":true}")) {
            clients.remove(client);
        }

        log.debug("SSE подписчик добавлен, всего: {}", clients.size());

        return emitter;
    }

    public void publishCard(Object event) {
        broadcast("card", gson.toJson(event));
    }

    private void broadcast(String eventName, String json) {
        if (clients.isEmpty()) {
            return;
        }
        clients.removeIf(client -> !client.send(eventName, json));
    }

    @Scheduled(fixedDelay = 20_000L)
    public void heartbeat() {
        broadcast("ping", "{}");
    }

    public int subscriberCount() {
        return clients.size();
    }

    /**
     * Обёртка над SseEmitter: сериализует запись и гарантирует,
     * что наружу не улетит ни одно исключение.
     */
    private static final class SseClient {

        private final SseEmitter emitter;

        private final ReentrantLock lock = new ReentrantLock();

        private final AtomicBoolean dead = new AtomicBoolean(false);

        private SseClient(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * @return true, если отправка удалась и клиента можно оставить в списке
         */
        private boolean send(String eventName, String json) {
            if (dead.get()) {
                return false;
            }

            lock.lock();
            try {
                if (dead.get()) {
                    return false;
                }
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(json, MediaType.APPLICATION_JSON)
                );
                return true;
            } catch (Exception exception) {
                // IOException (клиент закрыл вкладку / оборвал keep-alive),
                // IllegalStateException (emitter уже completed),
                // AsyncRequestNotUsableException
                log.debug("SSE клиент отключён: {}", exception.toString());
                markDead();
                return false;
            } finally {
                lock.unlock();
            }
        }

        private void markDead() {
            if (dead.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (Exception ignore) {
                    // уже завершён контейнером
                }
            }
        }
    }
}