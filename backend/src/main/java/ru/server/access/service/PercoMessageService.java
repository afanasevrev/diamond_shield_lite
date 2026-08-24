package ru.server.access.service;


import com.google.gson.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.server.access.dto.LiveDtos.LiveCardEvent;
import ru.server.access.entity.*;
import ru.server.access.repository.*;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class PercoMessageService {

    private static final Set<String> JOURNAL_EVENTS = Set.of(
            "card",
            "pass_personal",
            "pass_impersonal",
            "refusal_personal",
            "refusal_impersonal",
            "pass_ban_personal",
            "pass_ban_impersonal",
            "break",
            "exdev_long_open",
            "exdev_unlock",
            "input",
            "output"
    );

    private final AccessControllerRepository controllerRepository;
    private final PersonRepository personRepository;
    private final PassageEventRepository eventRepository;
    private final LiveEventService liveEventService;

    private PercoSocketService socketService;

    public PercoMessageService(
            AccessControllerRepository controllerRepository,
            PersonRepository personRepository,
            PassageEventRepository eventRepository,
            LiveEventService liveEventService
    ) {
        this.controllerRepository = controllerRepository;
        this.personRepository = personRepository;
        this.eventRepository = eventRepository;
        this.liveEventService = liveEventService;
    }

    public void setSocketService(PercoSocketService socketService) {
        this.socketService = socketService;
    }

    @Transactional
    public void process(Long controllerId, String rawMessage) {
        JsonObject root;

        try {
            root = JsonParser.parseString(rawMessage).getAsJsonObject();
        } catch (Exception exception) {
            System.err.println("Некорректный JSON от контроллера: " + rawMessage);
            return;
        }

        AccessController controller = controllerRepository.findById(controllerId)
                .orElse(null);

        if (controller == null) {
            return;
        }

        controller.setConnected(true);
        controller.setLastSeen(OffsetDateTime.now());

        processAuthorization(controller, root);

        if (!root.has("event")) {
            return;
        }

        String eventType = root.get("event").getAsString();

        if ("need_auth".equals(eventType)) {
            socketService.authorizeController(controller, root);
            return;
        }

        JsonObject payload = getObject(root, eventType);

        if (payload == null) {
            payload = new JsonObject();
        }

        String cardId = getString(payload, "id");
        Integer number = getInteger(payload, "number");
        Integer direction = getInteger(payload, "direction");
        Boolean removeCard = getBoolean(payload, "remove_card");
        String commandSource = getString(payload, "command_source");

        Person person = cardId == null
                ? null
                : personRepository.findByCardId(cardId).orElse(null);

        boolean allowed = person != null && person.isActive();

        if (JOURNAL_EVENTS.contains(eventType)) {
            PassageEvent event = new PassageEvent();
            event.setController(controller);
            event.setPerson(person);
            event.setEventType(eventType);
            event.setCardId(cardId);
            event.setDeviceNumber(number);
            event.setDirection(direction);
            event.setAllowed(allowed);
            event.setRemoveCard(removeCard);
            event.setCommandSource(commandSource);
            event.setRawJson(rawMessage);

            eventRepository.save(event);
        }

        if ("card".equals(eventType)) {
            handleCard(
                    controller,
                    person,
                    cardId,
                    number,
                    direction,
                    allowed
            );
        }
    }

    private void processAuthorization(
            AccessController controller,
            JsonObject root
    ) {
        if (!root.has("answer") || !root.get("answer").isJsonObject()) {
            return;
        }

        JsonObject answer = root.getAsJsonObject("answer");

        if (!answer.has("auth")) {
            return;
        }

        String result = answer.get("auth").getAsString();
        controller.setAuthenticated("ok".equalsIgnoreCase(result));
    }

    private void handleCard(
            AccessController controller,
            Person person,
            String cardId,
            Integer number,
            Integer direction,
            boolean allowed
    ) {
        String fullName = person == null
                ? "Неизвестная карта"
                : fullName(person);

        String photoUrl = person == null || person.getPhoto() == null
                ? null
                : "/api/persons/" + person.getId() + "/photo";

        liveEventService.publishCard(
                new LiveCardEvent(
                        "card",
                        cardId,
                        allowed,
                        person == null ? null : person.getId(),
                        fullName,
                        photoUrl,
                        controller.getId(),
                        controller.getName(),
                        number,
                        direction,
                        OffsetDateTime.now().toString()
                )
        );

        if (number == null || direction == null) {
            return;
        }

        try {
            if (allowed) {
                socketService.openOnce(
                        controller.getId(),
                        number,
                        direction,
                        3000
                );
            } else {
                socketService.denyAccess(
                        controller.getId(),
                        number,
                        direction
                );
            }
        } catch (IllegalStateException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private String fullName(Person person) {
        StringBuilder result = new StringBuilder()
                .append(person.getLastName())
                .append(" ")
                .append(person.getFirstName());

        if (person.getMiddleName() != null
                && !person.getMiddleName().isBlank()) {
            result.append(" ").append(person.getMiddleName());
        }

        return result.toString();
    }

    private JsonObject getObject(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonObject()) {
            return null;
        }

        return object.getAsJsonObject(name);
    }

    private String getString(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return null;
        }

        return object.get(name).getAsString();
    }

    private Integer getInteger(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return null;
        }

        return object.get(name).getAsInt();
    }

    private Boolean getBoolean(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return null;
        }

        return object.get(name).getAsBoolean();
    }
}