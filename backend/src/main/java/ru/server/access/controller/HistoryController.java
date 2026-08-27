package ru.server.access.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.server.access.dto.HistoryDtos.HistoryResponse;
import ru.server.access.entity.PassageEvent;
import ru.server.access.entity.Person;
import ru.server.access.repository.PassageEventRepository;
import ru.server.access.service.JournalExcelService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final PassageEventRepository eventRepository;
    private final JournalExcelService journalExcelService;

    public HistoryController(
            PassageEventRepository eventRepository,
            JournalExcelService journalExcelService
    ) {
        this.eventRepository = eventRepository;
        this.journalExcelService = journalExcelService;
    }

    @GetMapping
    public List<HistoryResponse> findHistory(@RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));

        return eventRepository.findAllByOrderByEventTimeDesc(
                        PageRequest.of(0, safeLimit)
                )
                .getContent()
                .stream()
                .map(this::map)
                .toList();
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportXlsx() {
        byte[] file = journalExcelService.exportJournal();

        String filename = "diamond-shield-journal-" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".xlsx";

        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument." + "spreadsheetml.sheet")
                )
                .contentLength(file.length)
                .cacheControl(CacheControl.noStore())
                .body(file);
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