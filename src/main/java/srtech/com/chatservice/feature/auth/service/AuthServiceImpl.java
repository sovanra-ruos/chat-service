package srtech.com.chatservice.feature.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Service;
import srtech.com.chatservice.feature.auth.repository.dto.AuthRequest;
import srtech.com.chatservice.feature.auth.repository.dto.AuthResponse;
import srtech.com.chatservice.feature.auth.repository.dto.Refresh;
import srtech.com.chatservice.security.TokenGenerator;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final DaoAuthenticationProvider daoAuthenticationProvider;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final TokenGenerator tokenGenerator;

    @Override
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = daoAuthenticationProvider
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        return tokenGenerator.generateTokens(authentication);
    }

    @Override
    public AuthResponse refresh(Refresh request) {
        Authentication authentication = jwtAuthenticationProvider
                .authenticate(new BearerTokenAuthenticationToken(request.refreshToken()));
        return tokenGenerator.generateTokens(authentication);
    }
}
