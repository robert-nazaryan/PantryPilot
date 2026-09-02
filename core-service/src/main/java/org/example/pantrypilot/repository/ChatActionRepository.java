package org.example.pantrypilot.repository;

import java.util.Optional;

import org.example.pantrypilot.model.ChatAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatActionRepository extends JpaRepository<ChatAction, Long> {

    @Query("SELECT a FROM ChatAction a WHERE a.id = :id AND a.session.user.id = :userId")
    Optional<ChatAction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
