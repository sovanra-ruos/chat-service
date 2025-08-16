package srtech.com.chatservice.feature.user.service;



import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import srtech.com.chatservice.domain.Role;
import srtech.com.chatservice.domain.User;
import srtech.com.chatservice.feature.user.mapper.UserMapper;
import srtech.com.chatservice.feature.user.repository.RoleRepository;
import srtech.com.chatservice.feature.user.repository.UserRepository;
import srtech.com.chatservice.feature.user.repository.dto.UserRequest;
import srtech.com.chatservice.feature.user.repository.dto.UserResponse;


import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;


    @Override
    public void register(UserRequest userRequest) {

        User user = userMapper.toEntity(userRequest);

        Role role = roleRepository.findByName("USER").orElseThrow(
                ()-> new NoSuchElementException("Role not found")
        );

        user.setUserName(userRequest.userName());
        user.setEmail(userRequest.email());
        user.setRoles(List.of(role));
        user.setProfileImage("default.jpg");
        user.setPassword(new BCryptPasswordEncoder().encode(userRequest.password()));
        user.setConfirm_password(new BCryptPasswordEncoder().encode(userRequest.confirm_password()));


        userRepository.save(user);
    }

    @Override
    public UserResponse getMe(Authentication authentication) {

        User user = userRepository.findUserByEmail(authentication.getName()).orElseThrow(
                ()-> new NoSuchElementException("User not found")
        );

        return userMapper.toResponse(user);
    }


}
