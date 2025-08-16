package srtech.com.chatservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import srtech.com.chatservice.feature.chat.ChatService;
import srtech.com.chatservice.feature.redis.RedisService;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final ChatService chatService;
    private final RedisService redisService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("New WebSocket connection established: {}", event.getMessage());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket connection closed: {}", sessionId);

        // Here you would typically handle user disconnection
        // For example, update user presence to OFFLINE
        // This requires additional session management to track user-session mappings
    }
}