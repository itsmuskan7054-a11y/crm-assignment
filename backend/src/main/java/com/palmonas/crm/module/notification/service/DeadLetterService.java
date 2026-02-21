package com.palmonas.crm.module.notification.service;

import com.palmonas.crm.module.notification.model.DeadLetterEntry;
import com.palmonas.crm.module.notification.repository.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private static final int MAX_RETRIES = 5;
    private final DeadLetterRepository repository;

    @Transactional
    public void record(String operationType, Map<String, Object> payload, Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        DeadLetterEntry entry = DeadLetterEntry.builder()
                .operationType(operationType)
                .payload(payload)
                .errorMessage(ex.getMessage() != null ? ex.getMessage() : "Unknown error")
                .stackTrace(sw.toString().substring(0, Math.min(sw.toString().length(), 4000)))
                .build();

        repository.save(entry);
        log.error("Dead letter recorded: {} - {}", operationType, ex.getMessage());
    }

    @Transactional(readOnly = true)
    public Page<DeadLetterEntry> getPending(int page, int size) {
        return repository.findByStatusOrderByCreatedAtDesc("PENDING", PageRequest.of(page, size));
    }

    @Transactional
    public void markResolved(java.util.UUID id) {
        repository.findById(id).ifPresent(entry -> {
            entry.setStatus("RESOLVED");
            repository.save(entry);
        });
    }

    @Transactional
    public void incrementRetry(java.util.UUID id) {
        repository.findById(id).ifPresent(entry -> {
            entry.setRetryCount(entry.getRetryCount() + 1);
            entry.setLastRetriedAt(Instant.now());
            if (entry.getRetryCount() >= MAX_RETRIES) {
                entry.setStatus("FAILED");
                log.warn("Dead letter entry {} exceeded max retries, marked as FAILED", id);
            } else {
                entry.setStatus("RETRIED");
            }
            repository.save(entry);
        });
    }

    public long getPendingCount() {
        return repository.countByStatus("PENDING");
    }
}
