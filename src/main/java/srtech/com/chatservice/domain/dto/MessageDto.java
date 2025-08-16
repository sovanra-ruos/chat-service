package srtech.com.chatservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import srtech.com.chatservice.domain.ChatMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private ChatMessage.MessageType messageType;
    private long timestamp;
}
