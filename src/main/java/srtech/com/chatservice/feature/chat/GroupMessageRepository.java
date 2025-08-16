package srtech.com.chatservice.feature.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import srtech.com.chatservice.domain.GroupMessage;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, String> {
}
