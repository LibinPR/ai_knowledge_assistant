package com.yourorg.aiknowledgeassistant.repository.career;

import com.yourorg.aiknowledgeassistant.domain.career.CareerMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerMatchRepository extends JpaRepository<CareerMatch, Long> {
    List<CareerMatch> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
