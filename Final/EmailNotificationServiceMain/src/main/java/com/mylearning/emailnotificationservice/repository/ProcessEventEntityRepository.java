package com.mylearning.emailnotificationservice.repository;

import com.mylearning.emailnotificationservice.entity.ProcessEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessEventEntityRepository extends JpaRepository<ProcessEventEntity, Long> {
    Optional<ProcessEventEntity> findByMessageId(String messageId);
}
