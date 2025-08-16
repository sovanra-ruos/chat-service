package srtech.com.chatservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import srtech.com.chatservice.domain.UserPresence;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPresenceDto {
    private String userId;
    private String username;
    private String roomId;
    private UserPresence.PresenceStatus status;
    private long timestamp;
}
