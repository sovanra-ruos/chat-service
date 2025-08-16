package srtech.com.chatservice.feature.user.repository.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record UserRequest(
        String userName,
        String email,
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
                message = "Password must contain at least one lowercase letter, one uppercase letter, one digit, one special character, and no whitespace,must be between 8 to 20 characters")
        String password,
        String confirm_password
) {
}
