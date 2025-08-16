package srtech.com.chatservice.feature.kafka;

import com.chatengine.avro.ChatMessageAvro;
import com.chatengine.avro.MessageType;
import com.chatengine.avro.PresenceStatus;
import com.chatengine.avro.UserPresenceAvro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendChatMessage(String id, String roomId, String senderId, String senderName,
                               String content, MessageType messageType, long timestamp) {

        ChatMessageAvro chatMessage = ChatMessageAvro.newBuilder()
                .setId(id)
                .setRoomId(roomId)
                .setSenderId(senderId)
                .setSenderName(senderName)
                .setContent(content)
                .setMessageType(messageType)
                .setTimestamp(timestamp)
                .build();

        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("chat-messages", roomId, chatMessage);

            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send chat message: {}", throwable.getMessage(), throwable);
                } else {
                    log.info("Successfully sent chat message: {} to topic: {} partition: {} offset: {}",
                            id, result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage(), e);
        }
    }

    public void sendUserPresence(String userId, String username, String roomId,
                                PresenceStatus status, long timestamp) {

        UserPresenceAvro userPresence = UserPresenceAvro.newBuilder()
                .setUserId(userId)
                .setUsername(username)
                .setRoomId(roomId)
                .setStatus(status)
                .setTimestamp(timestamp)
                .build();

        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("user-presence", userId, userPresence);

            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send user presence: {}", throwable.getMessage(), throwable);
                } else {
                    log.info("Successfully sent user presence: {} to topic: {} partition: {} offset: {}",
                            userId, result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Error sending user presence: {}", e.getMessage(), e);
        }
    }
}
