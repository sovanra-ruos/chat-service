package srtech.com.chatservice.feature.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import srtech.com.chatservice.domain.DirectMessage;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, String> {
    @Query("SELECT m FROM DirectMessage m WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2) OR (m.sender.id = :userId2 AND m.receiver.id = :userId1)")
    Page<DirectMessage> findMessagesBetweenUsers(@Param("userId1") String userId1, @Param("userId2") String userId2, Pageable pageable);
}
