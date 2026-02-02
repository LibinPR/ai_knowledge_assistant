package com.yourorg.aiknowledgeassistant.repository.audit;

import com.yourorg.aiknowledgeassistant.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditLog , Long> {
}
