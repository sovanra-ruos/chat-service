package srtech.com.chatservice.feature.auth.service;


import srtech.com.chatservice.feature.auth.repository.dto.AuthRequest;
import srtech.com.chatservice.feature.auth.repository.dto.AuthResponse;
import srtech.com.chatservice.feature.auth.repository.dto.Refresh;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    AuthResponse refresh(Refresh request);

}
