package srtech.com.chatservice.feature.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import srtech.com.chatservice.domain.ChatMessage;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId ORDER BY m.timestamp DESC")
    List<ChatMessage> findByRoomIdOrderByTimestampDesc(@Param("roomId") String roomId);

    @Query(value = "SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY timestamp DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findByRoomIdOrderByTimestampDesc(@Param("roomId") String roomId, @Param("limit") int limit);

    List<ChatMessage> findByRoomIdAndMessageTypeOrderByTimestampDesc(String roomId, ChatMessage.MessageType messageType);
}
