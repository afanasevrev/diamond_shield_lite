package ru.server.access.service;

import com.google.gson.*;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.server.access.entity.AccessController;
import ru.server.access.exception.*;
import ru.server.access.repository.AccessControllerRepository;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class PercoSocketService {

    private final AccessControllerRepository controllerRepository;
    private final PercoMessageService messageService;
    private final Gson gson;

    private final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final Set<Long> connecting = ConcurrentHashMap.newKeySet();

    public PercoSocketService(
            AccessControllerRepository controllerRepository,
            PercoMessageService messageService,
            Gson gson
    ) {
        this.controllerRepository = controllerRepository;
        this.messageService = messageService;
        this.gson = gson;
    }

    @PostConstruct
    public void initialize() {
        messageService.setSocketService(this);
    }

    @Scheduled(fixedDelay = 10000)
    public void reconnectEnabledControllers() {
        for (AccessController controller : controllerRepository.findAllByEnabledTrue()) {

            if (controller.getWebSocketUrl() == null || controller.getWebSocketUrl().isBlank()) {
                continue;
            }

            if (!isConnected(controller.getId()) && !connecting.contains(controller.getId())) {
                connect(controller.getId());
            }
        }
    }

    public void connect(Long controllerId) {
        AccessController controller = findController(controllerId);

        if (controller.getWebSocketUrl() == null
                || controller.getWebSocketUrl().isBlank()) {
            throw new BadRequestException(
                    "У контроллера не указан WebSocket URL"
            );
        }

        if (isConnected(controllerId) || !connecting.add(controllerId)) {
            return;
        }

        try {
            URI uri = URI.create(controller.getWebSocketUrl());

            webSocketClient.execute(new OutgoingControllerHandler(controllerId), String.valueOf(uri)).whenComplete((session, error) -> {
                connecting.remove(controllerId);

                if (error != null) {
                    markDisconnected(controllerId);
                    System.err.println(
                            "Не удалось подключиться к контроллеру "
                                    + controllerId + ": " + error.getMessage()
                    );
                }
            });
        } catch (Exception exception) {
            connecting.remove(controllerId);
            markDisconnected(controllerId);
            throw new BadRequestException(
                    "Некорректный WebSocket URL: " + exception.getMessage()
            );
        }
    }

    public boolean isConnected(Long controllerId) {
        WebSocketSession session = sessions.get(controllerId);
        return session != null && session.isOpen();
    }

    public void registerIncoming(
            Long controllerId,
            WebSocketSession session
    ) {
        WebSocketSession previous = sessions.put(controllerId, session);

        if (previous != null && previous.isOpen()
                && !previous.getId().equals(session.getId())) {
            try {
                previous.close(CloseStatus.NORMAL);
            } catch (Exception ignored) {
            }
        }

        markConnected(controllerId);
    }

    public void unregister(
            Long controllerId,
            WebSocketSession session
    ) {
        sessions.remove(controllerId, session);
        markDisconnected(controllerId);
    }

    public void send(Long controllerId, JsonObject message) {
        WebSocketSession session = sessions.get(controllerId);

        if (session == null || !session.isOpen()) {
            throw new IllegalStateException(
                    "Контроллер не подключён"
            );
        }

        try {
            synchronized (session) {
                session.sendMessage(
                        new TextMessage(gson.toJson(message))
                );
            }
        } catch (Exception exception) {
            unregister(controllerId, session);

            throw new IllegalStateException(
                    "Не удалось отправить сообщение контроллеру",
                    exception
            );
        }
    }

    public void authorizeController(
            AccessController controller,
            JsonObject root
    ) {
        JsonObject needAuth = root.getAsJsonObject("need_auth");

        if (needAuth == null || !needAuth.has("salt")) {
            return;
        }

        String salt = needAuth.get("salt").getAsString();
        String password = controller.getControllerPassword() == null
                ? ""
                : controller.getControllerPassword();

        JsonObject auth = new JsonObject();
        auth.addProperty("hash", md5(salt + password));

        JsonObject request = new JsonObject();
        request.addProperty("set", "auth");
        request.add("auth", auth);

        send(controller.getId(), request);
    }

    public void openOnce(
            Long controllerId,
            int number,
            int direction,
            int openTime
    ) {
        JsonObject exdev = new JsonObject();
        exdev.addProperty("number", number);
        exdev.addProperty("direction", direction);
        exdev.addProperty("action", "open");
        exdev.addProperty("open_type", "open once");
        exdev.addProperty("open_time", openTime);

        JsonObject root = new JsonObject();
        root.addProperty("control", "exdev");
        root.add("exdev", exdev);

        send(controllerId, root);
    }

    public void denyAccess(
            Long controllerId,
            int number,
            int direction
    ) {
        JsonObject access = new JsonObject();
        access.addProperty("number", number);
        access.addProperty("direction", direction);

        JsonObject root = new JsonObject();
        root.addProperty("control", "access");
        root.add("access", access);

        send(controllerId, root);
    }

    public Long resolveIncomingController(WebSocketSession session) {
        InetSocketAddress address = session.getRemoteAddress();

        if (address == null || address.getAddress() == null) {
            return null;
        }

        String ip = address.getAddress().getHostAddress();

        Optional<AccessController> controller =
                controllerRepository.findFirstByIp(ip);

        if (controller.isEmpty() && ip.startsWith("::ffff:")) {
            controller = controllerRepository.findFirstByIp(
                    ip.substring("::ffff:".length())
            );
        }

        return controller.map(AccessController::getId).orElse(null);
    }

    private AccessController findController(Long controllerId) {
        return controllerRepository.findById(controllerId)
                .orElseThrow(() ->
                        new NotFoundException("Контроллер не найден")
                );
    }

    private void markConnected(Long controllerId) {
        controllerRepository.findById(controllerId).ifPresent(controller -> {
            controller.setConnected(true);
            controller.setLastSeen(OffsetDateTime.now());
            controllerRepository.save(controller);
        });
    }

    private void markDisconnected(Long controllerId) {
        controllerRepository.findById(controllerId).ifPresent(controller -> {
            controller.setConnected(false);
            controller.setAuthenticated(false);
            controllerRepository.save(controller);
        });
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Не удалось вычислить MD5",
                    exception
            );
        }
    }

    private class OutgoingControllerHandler
            extends TextWebSocketHandler {

        private final Long controllerId;

        private OutgoingControllerHandler(Long controllerId) {
            this.controllerId = controllerId;
        }

        @Override
        public void afterConnectionEstablished(
                WebSocketSession session
        ) {
            connecting.remove(controllerId);
            registerIncoming(controllerId, session);
        }

        @Override
        protected void handleTextMessage(
                WebSocketSession session,
                TextMessage message
        ) {
            messageService.process(
                    controllerId,
                    message.getPayload()
            );
        }

        @Override
        public void afterConnectionClosed(
                WebSocketSession session,
                CloseStatus status
        ) {
            unregister(controllerId, session);
        }

        @Override
        public void handleTransportError(
                WebSocketSession session,
                Throwable exception
        ) {
            unregister(controllerId, session);
        }
    }
}