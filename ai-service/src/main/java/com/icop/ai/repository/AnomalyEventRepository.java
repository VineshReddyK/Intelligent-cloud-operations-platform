package com.icop.ai.repository;

import com.icop.ai.entity.AnomalyEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnomalyEventRepository extends JpaRepository<AnomalyEvent, UUID> {

    List<AnomalyEvent> findByServiceOrderByDetectedAtDesc(String service);

    List<AnomalyEvent> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);

    long countByServiceAndDetectedAtAfter(String service, Instant since);
}
