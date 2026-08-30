package com.civicpulse.modules.audit.service;

import com.civicpulse.modules.audit.model.AuditLog;
import com.civicpulse.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(UUID userId, String action, String resourceType, String resourceId, String details, String ipAddress) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("Recorded audit log: action=[{}] resource=[{}:{}]", action, resourceType, resourceId);
        } catch (Exception e) {
            log.error("Failed to persist audit log for action: {}", action, e);
        }
    }
}
