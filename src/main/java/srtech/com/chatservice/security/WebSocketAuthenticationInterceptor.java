package srtech.com.chatservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtAccessTokenDecoder;
    private final JwtToUserConverter jwtToUserConverter;

    // Store authentication by session ID to persist across messages
    private final ConcurrentHashMap<String, Authentication> sessionAuthentications = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();

        log.debug("Processing WebSocket message: command={}, sessionId={}", accessor.getCommand(), sessionId);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnection(accessor, sessionId);
        } else if (StompCommand.SEND.equals(accessor.getCommand()) ||
                  StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            setAuthenticationForMessage(accessor, sessionId);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            cleanupSession(sessionId);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor, String sessionId) {
        String authToken = getAuthToken(accessor);

        log.debug("Authenticating WebSocket connection for session {}, token present: {}",
                 sessionId, authToken != null);

        if (authToken != null && authToken.startsWith("Bearer ")) {
            try {
                String token = authToken.substring(7);
                Jwt jwt = jwtAccessTokenDecoder.decode(token);
                Authentication authentication = jwtToUserConverter.convert(jwt);

                if (authentication != null) {
                    accessor.setUser(authentication);
                    // Store authentication for this session
                    sessionAuthentications.put(sessionId, authentication);
                    log.info("WebSocket user authenticated for session {}: {}", sessionId, authentication.getName());
                    log.debug("Stored authentication in map. Total sessions: {}", sessionAuthentications.size());
                } else {
                    log.warn("JWT conversion failed for WebSocket connection session: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("WebSocket JWT authentication failed for session {}: {}", sessionId, e.getMessage());
            }
        } else {
            log.warn("No valid Authorization header found for WebSocket CONNECT session: {}", sessionId);
        }
    }

    private void setAuthenticationForMessage(StompHeaderAccessor accessor, String sessionId) {
        log.debug("Looking for authentication for session {}, stored sessions: {}",
                 sessionId, sessionAuthentications.keySet());

        Authentication authentication = sessionAuthentications.get(sessionId);
        if (authentication != null) {
            // Set the user on the accessor - this is what gets passed to @MessageMapping methods as Principal
            accessor.setUser(authentication);

            // Also set in SecurityContext for the current thread
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Store authentication in session attributes (more reliable than headers)
            accessor.getSessionAttributes().put("SPRING_SECURITY_AUTHENTICATION", authentication);

            // Ensure the message is mutable so we can modify it
            accessor.setLeaveMutable(true);

            log.info("Retrieved and set authentication for {} command in session {}: {}",
                     accessor.getCommand(), sessionId, authentication.getName());
        } else {
            log.warn("No authentication found for WebSocket session {} for command: {}. Available sessions: {}",
                   sessionId, accessor.getCommand(), sessionAuthentications.keySet());
        }
    }

    private void cleanupSession(String sessionId) {
        Authentication removed = sessionAuthentications.remove(sessionId);
        if (removed != null) {
            log.info("Cleaned up authentication for disconnected session: {}", sessionId);
        }
    }

    private String getAuthToken(StompHeaderAccessor accessor) {
        // Try to get token from native headers
        String authToken = accessor.getFirstNativeHeader("Authorization");
        if (authToken != null) {
            return authToken;
        }

        // Try to get from STOMP headers (case insensitive)
        authToken = accessor.getFirstNativeHeader("authorization");
        return authToken;
    }
}
