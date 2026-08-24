package ru.server.access.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.server.access.service.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration
        implements WebSocketConfigurer {

    private final PercoSocketService socketService;
    private final PercoMessageService messageService;

    public WebSocketConfiguration(
            PercoSocketService socketService,
            PercoMessageService messageService
    ) {
        this.socketService = socketService;
        this.messageService = messageService;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {
        registry
                .addHandler(
                        new IncomingControllerHandler(),
                        "/controller-ws"
                )
                .setAllowedOrigins("*");
    }

    private class IncomingControllerHandler
            extends TextWebSocketHandler {

        private final Map<String, Long> controllerIds =
                new ConcurrentHashMap<>();

        @Override
        public void afterConnectionEstablished(
                WebSocketSession session
        ) throws Exception {

            Long controllerId =
                    socketService.resolveIncomingController(session);

            if (controllerId == null) {
                session.close(
                        CloseStatus.POLICY_VIOLATION.withReason("IP контроллера не зарегистрирован")
                );
                return;
            }

            controllerIds.put(session.getId(), controllerId);
            socketService.registerIncoming(controllerId, session);
        }

        @Override
        protected void handleTextMessage(
                WebSocketSession session,
                TextMessage message
        ) {
            Long controllerId = controllerIds.get(session.getId());

            if (controllerId != null) {
                messageService.process(
                        controllerId,
                        message.getPayload()
                );
            }
        }

        @Override
        public void afterConnectionClosed(
                WebSocketSession session,
                CloseStatus status
        ) {
            Long controllerId =
                    controllerIds.remove(session.getId());

            if (controllerId != null) {
                socketService.unregister(controllerId, session);
            }
        }

        @Override
        public void handleTransportError(
                WebSocketSession session,
                Throwable exception
        ) throws Exception {
            Long controllerId =
                    controllerIds.remove(session.getId());

            if (controllerId != null) {
                socketService.unregister(controllerId, session);
            }

            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        }
    }
}