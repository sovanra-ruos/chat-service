package srtech.com.chatservice.feature.kafka;

import com.chatengine.avro.ChatMessageAvro;
import com.chatengine.avro.UserPresenceAvro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import srtech.com.chatservice.domain.ChatMessage;
import srtech.com.chatservice.domain.UserPresence;
import srtech.com.chatservice.domain.dto.MessageDto;
import srtech.com.chatservice.domain.dto.UserPresenceDto;
import srtech.com.chatservice.feature.chat.ChatService;
import srtech.com.chatservice.feature.redis.RedisService;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final RedisService redisService;

    @KafkaListener(topics = "chat-messages", groupId = "chat-service-group")
    public void consumeMessage(ChatMessageAvro chatMessageAvro) {
        try {
            log.info("Consumed message=[{}]", chatMessageAvro);

            MessageDto messageDto = new MessageDto(
                    chatMessageAvro.getId(),
                    chatMessageAvro.getRoomId(),
                    chatMessageAvro.getSenderId(),
                    chatMessageAvro.getSenderName(),
                    chatMessageAvro.getContent(),
                    ChatMessage.MessageType.valueOf(chatMessageAvro.getMessageType().name()),
                    chatMessageAvro.getTimestamp()
            );

            // save to db asynchronously
            chatService.saveMessage(messageDto);

            redisService.cacheRecentMessage(messageDto);

            // Broadcast to WebSocket subscribers
            messagingTemplate.convertAndSend(
                    "/topic/room/" + messageDto.getRoomId(),
                    messageDto
            );

        } catch (Exception e) {
            log.error("Error consuming message: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user-presence", groupId = "chat-service-group")
    public void consumeUserPresence(UserPresenceAvro userPresenceAvro){
        try {
            log.info("Consumed user presence=[{}]", userPresenceAvro);

            UserPresenceDto userPresenceDto = new UserPresenceDto(
                    userPresenceAvro.getUserId(),
                    userPresenceAvro.getUsername(),
                    userPresenceAvro.getRoomId(),
                    UserPresence.PresenceStatus.valueOf(userPresenceAvro.getStatus().name()),
                    userPresenceAvro.getTimestamp()
            );

            redisService.updateUserPresence(userPresenceDto);

            // Broadcast user presence status to WebSocket subscribers in room
            messagingTemplate.convertAndSend(
                    "/topic/room/" + userPresenceDto.getRoomId() + "/presence",
                    userPresenceDto
            );
        }catch (Exception e){
            log.error("Error consuming user presence message: {}", e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "chat-messages", groupId = "chat-service-group")
    public void consumeChatMessage(
            @Payload ChatMessageAvro chatMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received chat message from topic: {}, partition: {}, offset: {}", topic, partition, offset);
            log.info("Chat message: id={}, roomId={}, senderId={}, content={}, type={}, timestamp={}",
                    chatMessage.getId(),
                    chatMessage.getRoomId(),
                    chatMessage.getSenderId(),
                    chatMessage.getContent(),
                    chatMessage.getMessageType(),
                    chatMessage.getTimestamp());

            // Process the chat message here
            // For example: broadcast to WebSocket clients, save to database, etc.

            // Acknowledge successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
            // Don't acknowledge - this will trigger retry logic
        }
    }

    @KafkaListener(topics = "user-presence", groupId = "chat-service-group")
    public void consumeUserPresence(
            @Payload UserPresenceAvro userPresence,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received user presence from topic: {}, partition: {}, offset: {}", topic, partition, offset);
            log.info("User presence: userId={}, username={}, roomId={}, status={}, timestamp={}",
                    userPresence.getUserId(),
                    userPresence.getUsername(),
                    userPresence.getRoomId(),
                    userPresence.getStatus(),
                    userPresence.getTimestamp());

            // Process the user presence update here
            // For example: update user status in Redis, broadcast to WebSocket clients, etc.

            // Acknowledge successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing user presence: {}", e.getMessage(), e);
            // Don't acknowledge - this will trigger retry logic
        }
    }
}
