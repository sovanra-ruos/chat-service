package srtech.com.chatservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }

}
