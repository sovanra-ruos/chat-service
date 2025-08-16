package srtech.com.chatservice.feature.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import srtech.com.chatservice.domain.UserPresence;
import srtech.com.chatservice.domain.dto.MessageDto;
import srtech.com.chatservice.domain.dto.UserPresenceDto;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String , Object> redisTemplate;

    private static final String USER_PRESENCE_PREFIX = "presence:";
    private static final String ROOM_USERS_PREFIX = "room:users:";
    private static final String RECENT_MESSAGES_PREFIX = "room:messages:";
    private static final String USER_SESSIONS_PREFIX = "user:session:";

    public void updateUserPresence(UserPresenceDto presenceDto){
        try {
            String key = USER_PRESENCE_PREFIX + presenceDto.getUserId();
            redisTemplate.opsForValue().set(key, presenceDto, Duration.ofHours(24));

            String roomUsersKey = ROOM_USERS_PREFIX + presenceDto.getRoomId();

            if(presenceDto.getStatus() == UserPresence.PresenceStatus.ONLINE){
                redisTemplate.opsForSet().add(roomUsersKey, presenceDto.getUserId());
            } else if(presenceDto.getStatus() == UserPresence.PresenceStatus.OFFLINE) {
                redisTemplate.opsForSet().remove(roomUsersKey, presenceDto.getUserId());
            }

            log.debug("Updated user presence in Redis: {}", presenceDto.getUserId());

        }catch (Exception e){
            log.error("Error updating user presence in Redis: {}", e.getMessage(), e);
        }

    }

    public UserPresenceDto getUserPresence(String userId) {
        try {
            String key = USER_PRESENCE_PREFIX + userId;
            return (UserPresenceDto) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error retrieving user presence from Redis: {}", e.getMessage(), e);
            return null;
        }
    }

    public Set<Object> getRoomUsers(String roomId) {
        try {
            String key = ROOM_USERS_PREFIX + roomId;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Error retrieving room users from Redis: {}", e.getMessage(), e);
            return Set.of();
        }
    }

    public void cacheRecentMessage(MessageDto messageDto) {
        try {
            String key = RECENT_MESSAGES_PREFIX + messageDto.getRoomId();
            redisTemplate.opsForList().leftPush(key, messageDto);
            redisTemplate.opsForList().trim(key, 0, 49); // Keep last 50 messages
            redisTemplate.expire(key, Duration.ofDays(7));

            log.debug("Cached recent message for room: {}", messageDto.getRoomId());
        }catch (Exception e){
            log.error("Error caching recent message in Redis: {}", e.getMessage(), e);
        }
    }

    public List<Object> getRecentMessages(String roomId) {
        try {
            String key = RECENT_MESSAGES_PREFIX + roomId;
            return redisTemplate.opsForList().range(key, 0, -1);
        } catch (Exception e) {
            log.error("Error getting recent messages from Redis: {}", e.getMessage());
            return List.of();
        }
    }

    public void storeUserSession(String userId, String sessionId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForValue().set(key, sessionId, Duration.ofHours(24));
        } catch (Exception e) {
            log.error("Error storing user session in Redis: {}", e.getMessage());
        }
    }

    public String getUserSession(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error getting user session from Redis: {}", e.getMessage());
            return null;
        }
    }

    public void removeUserSession(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Error removing user session from Redis: {}", e.getMessage());
        }
    }

}
