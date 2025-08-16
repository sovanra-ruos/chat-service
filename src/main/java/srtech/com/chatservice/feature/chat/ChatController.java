package srtech.com.chatservice.feature.chat;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import srtech.com.chatservice.domain.ChatMessage;
import srtech.com.chatservice.domain.ChatRoom;
import srtech.com.chatservice.domain.UserPresence;
import srtech.com.chatservice.feature.redis.RedisService;
import srtech.com.chatservice.security.CustomUserDetail;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final RedisService redisService;

    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload Map<String , String> message,
                           SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        try {
            String senderId = message.get("senderId");
            String senderName = message.get("senderName");
            String content = message.get("content");

            // Enhanced authentication check - try multiple sources
            Authentication authentication = getAuthenticationFromMessage(headerAccessor, principal);

            if (authentication == null) {
                log.warn("Unauthenticated user attempted to send message to room {} - no authentication found", roomId);
                return;
            }

            // Verify user identity if possible
            if (authentication.getPrincipal() instanceof CustomUserDetail) {
                CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
                String authenticatedUserId = userDetails.getUser().getId().toString();

                if (senderId != null && !senderId.equals(authenticatedUserId)) {
                    log.warn("User {} attempted to send message as user {}", authenticatedUserId, senderId);
                    return;
                }
            }

            redisService.storeUserSession(senderId, headerAccessor.getSessionId());
            chatService.sendMessage(roomId,senderId,senderName,content, ChatMessage.MessageType.CHAT);
            log.info("Message sent from user {} to room {}: {}", senderName, roomId, content);

        }catch (Exception e){
            log.error("Error sending message to room {}: {}", roomId, e.getMessage());
        }
    }

    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId, @Payload Map<String ,String > user,
                       SimpMessageHeaderAccessor headerAccessor, Principal principal) {

        try {
            String userId = user.get("userId");
            String username = user.get("username");

            // Enhanced authentication check - try multiple sources
            Authentication authentication = getAuthenticationFromMessage(headerAccessor, principal);

            log.debug("addUser called for room {}: userId={}, username={}, authentication={}",
                     roomId, userId, username, authentication != null ? "present" : "null");

            if (authentication == null) {
                log.warn("Unauthenticated user attempted to join room {} - no authentication found", roomId);
                return;
            }

            // Verify user identity if possible
            if (authentication.getPrincipal() instanceof CustomUserDetail) {
                CustomUserDetail userDetails = (CustomUserDetail) authentication.getPrincipal();
                String authenticatedUserId = userDetails.getUser().getId().toString();

                log.debug("Authenticated user ID: {}, provided userId: {}", authenticatedUserId, userId);

                if (userId != null && !userId.equals(authenticatedUserId)) {
                    log.warn("User {} attempted to join room as user {}", authenticatedUserId, userId);
                    return;
                }
            }

            // store session info to redis
            redisService.storeUserSession(userId, headerAccessor.getSessionId());
            chatService.joinRoom(roomId, userId);

            // update user presence
            chatService.updateUserPresence(userId,username, roomId, UserPresence.PresenceStatus.ONLINE);

            // send enhanced join message
            String joinMessage = String.format("🎉 %s has joined the conversation! Welcome!", username);
            chatService.sendMessage(roomId, userId, username, joinMessage, ChatMessage.MessageType.JOIN);

            log.info("User {} successfully joined room {}", username, roomId);

        }catch (Exception e){
            log.error("Error adding user to room {}: {}", roomId, e.getMessage(), e);
        }
    }

    @SubscribeMapping("/topic/room/{roomId}")
    public void subscribeToRoom(@DestinationVariable String roomId, Principal principal) {
        if (principal != null) {
            log.info("User {} subscribed to room: {}", principal.getName(), roomId);
        } else {
            log.warn("Unauthenticated user attempted to subscribe to room: {}", roomId);
        }
    }

    @SubscribeMapping("/topic/room/{roomId}/presence")
    public void subscribeToRoomPresence(@DestinationVariable String roomId, Principal principal) {
        if (principal != null) {
            log.info("User {} subscribed to room presence: {}", principal.getName(), roomId);
        } else {
            log.warn("Unauthenticated user attempted to subscribe to room presence: {}", roomId);
        }
    }

    // REST API endpoints for chat functionality
    @GetMapping("/api/v1/rooms")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatRoom>> getAllRooms(Authentication authentication) {
        List<ChatRoom> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/api/v1/rooms")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody Map<String,String> roomData, Authentication authentication) {
        try{
            String name = roomData.get("name");
            String description = roomData.get("description");
            ChatRoom newRoom = chatService.createRoom(name, description);
            return ResponseEntity.ok(newRoom);

        }catch (Exception e){
            log.error("Error creating room: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/v1/rooms/{roomId}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoom> getRoom(@PathVariable String roomId, Authentication authentication) {
        return chatService.getRoom(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/rooms/{roomId}/messages")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessage>> getRoomMessages(@PathVariable String roomId,
                                                             @RequestParam(defaultValue = "50") int limit,
                                                             Authentication authentication) {
        try {
            List<ChatMessage> messages = chatService.getRoomMessages(roomId, limit);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error fetching messages for room {}: {}", roomId, e.getMessage());
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    @GetMapping("/api/v1/rooms/{roomId}/users")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Set<Object>> getRoomUsers(@PathVariable String roomId, Authentication authentication) {
        Set<Object> users = redisService.getRoomUsers(roomId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/api/v1/rooms/{roomId}/recent-messages")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Object>> getRecentMessages(@PathVariable String roomId, Authentication authentication) {
        List<Object> messages = redisService.getRecentMessages(roomId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get authentication from multiple sources - Principal parameter, session attributes, or message headers
     */
    private Authentication getAuthenticationFromMessage(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        // First try the Principal parameter (this should work after our interceptor fix)
        if (principal instanceof Authentication) {
            log.debug("Found authentication via Principal parameter: {}", principal.getName());
            return (Authentication) principal;
        }

        // Fallback to session attributes (set by our interceptor)
        Object authFromSession = headerAccessor.getSessionAttributes().get("SPRING_SECURITY_AUTHENTICATION");
        if (authFromSession instanceof Authentication) {
            log.debug("Found authentication via session attributes: {}", ((Authentication) authFromSession).getName());
            return (Authentication) authFromSession;
        }

        // Fallback to message headers (legacy approach)
        Object authFromHeaders = headerAccessor.getHeader("SPRING_SECURITY_CONTEXT");
        if (authFromHeaders instanceof Authentication) {
            log.debug("Found authentication via message headers: {}", ((Authentication) authFromHeaders).getName());
            return (Authentication) authFromHeaders;
        }

        // Last resort - try SecurityContext
        Authentication contextAuth = SecurityContextHolder.getContext().getAuthentication();
        if (contextAuth != null && contextAuth.isAuthenticated()) {
            log.debug("Found authentication via SecurityContext: {}", contextAuth.getName());
            return contextAuth;
        }

        log.debug("No authentication found in any source");
        return null;
    }
}
