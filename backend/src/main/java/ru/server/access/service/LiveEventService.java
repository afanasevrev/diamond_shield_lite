package ru.server.access.service;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveEventService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final Gson gson;

    public LiveEventService(Gson gson) {
        this.gson = gson;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
        });

        emitter.onError(error -> {
            emitters.remove(emitter);
        });

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data("{\"connected\":true}")
            );
        } catch (Exception exception) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void publishCard(Object event) {
        String json = gson.toJson(event);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("card")
                                .data(json)
                );
            } catch (Exception exception) {
                emitters.remove(emitter);
            }
        }
    }
}