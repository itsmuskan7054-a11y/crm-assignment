package com.palmonas.crm.module.notification.repository;

import com.palmonas.crm.module.notification.model.DeadLetterEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetterEntry, UUID> {

    Page<DeadLetterEntry> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<DeadLetterEntry> findByStatusAndRetryCountLessThan(String status, int maxRetries);

    long countByStatus(String status);
}
