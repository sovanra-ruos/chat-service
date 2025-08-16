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

            // Convert Avro message to DTO
            MessageDto messageDto = new MessageDto(
                    chatMessage.getId(),
                    chatMessage.getRoomId(),
                    chatMessage.getSenderId(),
                    chatMessage.getSenderName(),
                    chatMessage.getContent(),
                    ChatMessage.MessageType.valueOf(chatMessage.getMessageType().name()),
                    chatMessage.getTimestamp()
            );

            // Save to database asynchronously
            chatService.saveMessage(messageDto);

            // Cache in Redis
            redisService.cacheRecentMessage(messageDto);

            // **CRITICAL**: Broadcast to WebSocket subscribers
            String destination = "/topic/room/" + messageDto.getRoomId();
            log.info("Broadcasting message to WebSocket destination: {}", destination);
            messagingTemplate.convertAndSend(destination, messageDto);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.debug("Successfully processed and broadcasted chat message: {}", chatMessage.getId());

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

            // Convert Avro message to DTO
            UserPresenceDto userPresenceDto = new UserPresenceDto(
                    userPresence.getUserId(),
                    userPresence.getUsername(),
                    userPresence.getRoomId(),
                    UserPresence.PresenceStatus.valueOf(userPresence.getStatus().name()),
                    userPresence.getTimestamp()
            );

            // Update Redis cache
            redisService.updateUserPresence(userPresenceDto);

            // Broadcast user presence to WebSocket subscribers
            String destination = "/topic/room/" + userPresenceDto.getRoomId() + "/presence";
            log.info("Broadcasting presence update to WebSocket destination: {}", destination);
            messagingTemplate.convertAndSend(destination, userPresenceDto);

            // Acknowledge successful processing
            acknowledgment.acknowledge();
            log.debug("Successfully processed and broadcasted user presence: {}", userPresence.getUserId());

        } catch (Exception e) {
            log.error("Error processing user presence: {}", e.getMessage(), e);
            // Don't acknowledge - this will trigger retry logic
        }
    }
}
