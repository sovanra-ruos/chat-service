package srtech.com.chatservice.feature.user.service;



import org.springframework.security.core.Authentication;
import srtech.com.chatservice.feature.user.repository.dto.UserRequest;
import srtech.com.chatservice.feature.user.repository.dto.UserResponse;


public interface UserService {

    void register(UserRequest userRequest);

    UserResponse getMe (Authentication authentication);

}
