package srtech.com.chatservice.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import srtech.com.chatservice.domain.User;
import srtech.com.chatservice.feature.user.repository.UserRepository;


@Getter
@Setter
@RequiredArgsConstructor
@Component
public class JwtToUserConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserRepository userRepository;


    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt source) {
        User user = userRepository.findUserByEmail(source.getSubject()).orElseThrow(()-> new BadCredentialsException("Invalid Token"));
        CustomUserDetail userDetail = new CustomUserDetail();
        userDetail.setUser(user);

        return new UsernamePasswordAuthenticationToken(userDetail,"",userDetail.getAuthorities());
    }
}
