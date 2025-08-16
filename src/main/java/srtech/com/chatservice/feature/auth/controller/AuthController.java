package srtech.com.chatservice.feature.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import srtech.com.chatservice.feature.auth.repository.dto.AuthRequest;
import srtech.com.chatservice.feature.auth.repository.dto.AuthResponse;
import srtech.com.chatservice.feature.auth.repository.dto.Refresh;
import srtech.com.chatservice.feature.auth.service.AuthServiceImpl;
import srtech.com.chatservice.feature.user.repository.dto.UserRequest;
import srtech.com.chatservice.feature.user.service.UserService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthServiceImpl authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRequest userRequest) {
        userService.register(userRequest);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(authService.login(authRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Refresh refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

}
