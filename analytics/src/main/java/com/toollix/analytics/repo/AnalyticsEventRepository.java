package com.toollix.analytics.repo;

import com.toollix.analytics.model.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    @Query("SELECT COUNT(e) FROM AnalyticsEvent e WHERE e.profileId = :profileId AND e.eventType = :eventType AND e.occurredAt >= :since")
    long countEventsSince(@Param("profileId") Long profileId, @Param("eventType") String eventType,
            @Param("since") Instant since);

    @Query("SELECT e FROM AnalyticsEvent e WHERE e.profileId = :profileId AND e.occurredAt >= :since ORDER BY e.occurredAt ASC")
    List<AnalyticsEvent> findEventsSince(@Param("profileId") Long profileId, @Param("since") Instant since);
}
