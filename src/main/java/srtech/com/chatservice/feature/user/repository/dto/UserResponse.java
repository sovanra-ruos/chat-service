package srtech.com.chatservice.feature.user.repository.dto;

import lombok.Builder;

@Builder
public record UserResponse(
        String username,
        String email
) {
}
