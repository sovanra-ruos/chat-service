package srtech.com.chatservice.feature.chat;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import srtech.com.chatservice.domain.ChatMessage;
import srtech.com.chatservice.domain.ChatRoom;
import srtech.com.chatservice.domain.UserPresence;
import srtech.com.chatservice.domain.dto.MessageDto;
import srtech.com.chatservice.domain.dto.UserPresenceDto;
import srtech.com.chatservice.feature.kafka.KafkaProducerService;
import srtech.com.chatservice.feature.redis.RedisService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final KafkaProducerService kafkaProducerService;
    private final RedisService redisService;

    public MessageDto sendMessage(String roomId, String senderId, String senderName, String content, ChatMessage.MessageType messageType) {

        try {

            MessageDto messageDto = new MessageDto(
                    UUID.randomUUID().toString(), roomId, senderId, senderName, content, messageType, Instant.now().toEpochMilli()
            );

            // Convert messageType to Avro MessageType
            com.chatengine.avro.MessageType avroMessageType = com.chatengine.avro.MessageType.valueOf(messageType.name());

            kafkaProducerService.sendChatMessage(
                    messageDto.getId(),
                    messageDto.getRoomId(),
                    messageDto.getSenderId(),
                    messageDto.getSenderName(),
                    messageDto.getContent(),
                    avroMessageType,
                    messageDto.getTimestamp()
            );
            log.info("Message sent to Kafka: {}", messageDto);

            return messageDto;

        }catch (Exception e){
            log.error("Error sending message: {}", e.getMessage());
            throw new RuntimeException("Failed to send message", e);
        }

    }

    public void saveMessage(MessageDto messageDto) {
        try {
            // Skip saving JOIN and LEAVE messages to database - they are just notifications
            if (messageDto.getMessageType() == ChatMessage.MessageType.JOIN ||
                messageDto.getMessageType() == ChatMessage.MessageType.LEAVE) {
                log.info("Skipping database save for notification message type: {} - ID: {}",
                         messageDto.getMessageType(), messageDto.getId());
                return;
            }

            ChatMessage chatMessage = new ChatMessage();

            chatMessage.setId(messageDto.getId());
            chatMessage.setRoomId(messageDto.getRoomId());
            chatMessage.setSenderId(messageDto.getSenderId());
            chatMessage.setSenderName(messageDto.getSenderName());
            chatMessage.setContent(messageDto.getContent());
            chatMessage.setMessageType(messageDto.getMessageType());
            chatMessage.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(messageDto.getTimestamp()), ZoneOffset.UTC));

            messageRepository.save(chatMessage);
            log.info("Message saved to database: {}", messageDto.getId());

        }catch (Exception e){
            log.error("Error saving message: {}", e.getMessage());
        }
    }

    public List<ChatMessage> getRoomMessages(String roomId, int limit) {
        try {
            return messageRepository.findByRoomIdOrderByTimestampDesc(roomId, limit);
        } catch (Exception e) {
            log.error("Error fetching room messages: {}", e.getMessage());
            return List.of();
        }
    }

    public ChatRoom createRoom(String name, String description) {
        try {
            ChatRoom room = new ChatRoom();
            room.setId(UUID.randomUUID().toString());
            room.setName(name);
            room.setDescription(description);
            room.setCreatedAt(LocalDateTime.now());
            room.setParticipants(Set.of()); // Initialize with no participants

            return roomRepository.save(room);
        }catch (Exception e){
            log.error("Error creating room: {}", e.getMessage());
            throw new RuntimeException("Failed to create room", e);
        }
    }

    public Optional<ChatRoom> getRoom(String roomId) {
        return roomRepository.findById(roomId);
    }

    public List<ChatRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    public void joinRoom(String roomId, String userId) {
        try {
            Optional<ChatRoom> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                ChatRoom room = roomOpt.get();
                room.getParticipants().add(userId);
                roomRepository.save(room);

                log.info("User {} joined room {}", userId, roomId);
            }
        } catch (Exception e) {
            log.error("Error joining room: {}", e.getMessage());
        }
    }

    public void leaveRoom(String roomId, String userId) {
        try {
            Optional<ChatRoom> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                ChatRoom room = roomOpt.get();
                room.getParticipants().remove(userId);
                roomRepository.save(room);

                log.info("User {} left room {}", userId, roomId);
            }
        } catch (Exception e) {
            log.error("Error leaving room: {}", e.getMessage());
        }
    }

    public void updateUserPresence(String userId, String username, String roomId, UserPresence.PresenceStatus status) {
        try {
            UserPresenceDto presenceDto = new UserPresenceDto(
                    userId,
                    username,
                    roomId,
                    status,
                    Instant.now().toEpochMilli()
            );

            // Convert status to Avro PresenceStatus
            com.chatengine.avro.PresenceStatus avroStatus = com.chatengine.avro.PresenceStatus.valueOf(status.name());

            kafkaProducerService.sendUserPresence(
                    presenceDto.getUserId(),
                    presenceDto.getUsername(),
                    presenceDto.getRoomId(),
                    avroStatus,
                    presenceDto.getTimestamp()
            );
            log.info("User presence updated for user: {} in room: {}", userId, roomId);

        } catch (Exception e) {
            log.error("Error updating user presence: {}", e.getMessage());
        }
    }

}