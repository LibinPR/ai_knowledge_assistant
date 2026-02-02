package com.yourorg.aiknowledgeassistant.service.audit;

import com.yourorg.aiknowledgeassistant.domain.audit.AuditLog;
import com.yourorg.aiknowledgeassistant.repository.audit.AuditRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void log(
            Long actorUserId,
            String action,
            String entity,
            Long entityId,
            String metadata
    ) {
        AuditLog log = new AuditLog(
                actorUserId,
                action,
                entity,
                entityId,
                metadata
        );
        auditRepository.save(log);
    }
}
