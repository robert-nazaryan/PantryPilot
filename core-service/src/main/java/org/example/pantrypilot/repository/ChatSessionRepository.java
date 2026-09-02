package org.example.pantrypilot.repository;

import java.util.Optional;

import org.example.pantrypilot.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT s FROM ChatSession s LEFT JOIN FETCH s.messages "
            + "WHERE s.id = :id AND s.user.id = :userId")
    Optional<ChatSession> findByIdAndUserIdWithMessages(
            @Param("id") Long id, @Param("userId") Long userId);
}
