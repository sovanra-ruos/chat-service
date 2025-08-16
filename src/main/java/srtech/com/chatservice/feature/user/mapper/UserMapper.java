package srtech.com.chatservice.feature.user.mapper;



import org.mapstruct.Mapper;
import srtech.com.chatservice.domain.User;
import srtech.com.chatservice.feature.user.repository.dto.UserRequest;
import srtech.com.chatservice.feature.user.repository.dto.UserResponse;


@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRequest userRequest);

    UserResponse toResponse(User user);

}
