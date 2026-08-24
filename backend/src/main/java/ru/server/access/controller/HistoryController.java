package ru.server.access.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import ru.server.access.dto.HistoryDtos.HistoryResponse;
import ru.server.access.entity.*;
import ru.server.access.repository.PassageEventRepository;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final PassageEventRepository eventRepository;

    public HistoryController(
            PassageEventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public List<HistoryResponse> findHistory(
            @RequestParam(defaultValue = "200") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));

        return eventRepository.findAllByOrderByEventTimeDesc(
                        PageRequest.of(0, safeLimit)
                )
                .getContent()
                .stream()
                .map(this::map)
                .toList();
    }

    private HistoryResponse map(PassageEvent event) {
        return new HistoryResponse(
                event.getId(),
                event.getEventType(),
                event.getCardId(),
                event.getPerson() == null
                        ? null
                        : fullName(event.getPerson()),
                event.getController() == null
                        ? null
                        : event.getController().getName(),
                event.getDeviceNumber(),
                event.getDirection(),
                event.isAllowed(),
                event.getRemoveCard(),
                event.getCommandSource(),
                event.getEventTime().toString()
        );
    }

    private String fullName(Person person) {
        String result =
                person.getLastName() + " " + person.getFirstName();

        if (person.getMiddleName() != null
                && !person.getMiddleName().isBlank()) {
            result += " " + person.getMiddleName();
        }

        return result;
    }
}