package srtech.com.chatservice.feature.chat;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import srtech.com.chatservice.domain.ChatMessage;
import srtech.com.chatservice.domain.ChatRoom;
import srtech.com.chatservice.domain.UserPresence;
import srtech.com.chatservice.feature.redis.RedisService;

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
    public void sendMessage(@DestinationVariable String roomId, @Payload Map<String , String> message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String senderId = message.get("senderId");
            String senderName = message.get("senderName");
            String content = message.get("content");


            redisService.storeUserSession(senderId, headerAccessor.getSessionId());

            chatService.sendMessage(roomId,senderId,senderName,content, ChatMessage.MessageType.CHAT);

            log.info("Message sent from user {} to room {}: {}", senderName, roomId, content);

        }catch (Exception e){
            log.error("Error sending message to room {}: {}", roomId, e.getMessage());
        }

    }

    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId, @Payload Map<String ,String > user, SimpMessageHeaderAccessor headerAccessor) {

        try {
            String userId = user.get("userId");
            String username = user.get("username");

            // store session info to redis
            redisService.storeUserSession(userId, headerAccessor.getSessionId());

            chatService.joinRoom(roomId, userId);

            // update user presence
            chatService.updateUserPresence(userId,username, roomId, UserPresence.PresenceStatus.ONLINE);

            // send join message
            chatService.sendMessage(roomId, userId, username, username + " joined the chat!", ChatMessage.MessageType.JOIN);

            log.info("User {} joined room {}", username, roomId);

        }catch (Exception e){
            log.error("Error adding user to room {}: {}", roomId, e.getMessage());
        }
    }

    @SubscribeMapping("/topic/room/{roomId}")
    public void subscribeToRoom(@DestinationVariable String roomId, Principal principal) {
        log.info("User subscribed to room: {}", roomId);
    }

    @SubscribeMapping("/topic/room/{roomId}/presence")
    public void subscribeToRoomPresence(@DestinationVariable String roomId, Principal principal) {
        log.info("User subscribed to room presence: {}", roomId);
    }

    // rest api endpoints can be added here for user presence and message history if needed

    @GetMapping("/api/rooms")
    @ResponseBody
    public ResponseEntity<List<ChatRoom>> getAllRooms() {
        List<ChatRoom> rooms = chatService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/api/rooms")
    @ResponseBody
    public ResponseEntity<ChatRoom> createRoom(@RequestBody Map<String,String> roomData) {
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

    @GetMapping("/api/rooms/{roomId}")
    @ResponseBody
    public ResponseEntity<ChatRoom> getRoom(@PathVariable String roomId) {
        return chatService.getRoom(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/rooms/{roomId}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getRoomMessages(@PathVariable String roomId,
                                                             @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessage> messages = chatService.getRoomMessages(roomId, limit);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/rooms/{roomId}/users")
    @ResponseBody
    public ResponseEntity<Set<Object>> getRoomUsers(@PathVariable String roomId) {
        Set<Object> users = redisService.getRoomUsers(roomId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/api/rooms/{roomId}/recent-messages")
    @ResponseBody
    public ResponseEntity<List<Object>> getRecentMessages(@PathVariable String roomId) {
        List<Object> messages = redisService.getRecentMessages(roomId);
        return ResponseEntity.ok(messages);
    }


}
