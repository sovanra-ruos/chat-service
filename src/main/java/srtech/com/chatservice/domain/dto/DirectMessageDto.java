package srtech.com.chatservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectMessageDto {
    private String id;
    private String senderId;
    private String receiverId;
    private String senderName;
    private String content;
    private srtech.com.chatservice.domain.DirectMessage.MessageType messageType;
    private long timestamp;
    private String conversationId;
}
