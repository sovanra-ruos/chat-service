package srtech.com.chatservice.feature.auth.repository.dto;

import lombok.Builder;

@Builder
public record Refresh(
        String refreshToken
) {
}
