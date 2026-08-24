package ru.server.access.controller;


import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.server.access.service.LiveEventService;

@RestController
@RequestMapping("/api/live")
public class LiveController {

    private final LiveEventService liveEventService;

    public LiveController(LiveEventService liveEventService) {
        this.liveEventService = liveEventService;
    }

    @GetMapping(value = "/cards", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToCards() {
        return liveEventService.subscribe();
    }
}