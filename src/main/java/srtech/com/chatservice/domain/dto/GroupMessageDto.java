package srtech.com.chatservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMessageDto {
    private String id;
    private String groupId;
    private String senderId;
    private String content;
    private long timestamp;
}

