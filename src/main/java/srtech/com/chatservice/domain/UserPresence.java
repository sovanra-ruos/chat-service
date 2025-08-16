package srtech.com.chatservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPresence {
    private String userId;
    private String username;
    private String roomId;
    private PresenceStatus status;
    private LocalDateTime timestamp;

    public enum PresenceStatus {
        ONLINE, OFFLINE, AWAY
    }
}