package com.toollix.analytics.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "nfc_card_id")
    private Long nfcCardId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "occurred_at")
    private Instant occurredAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    public AnalyticsEvent() {
    }

    public Long getId() {
        return id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getNfcCardId() {
        return nfcCardId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMeta() {
        return meta;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
<<<<<<< HEAD
=======

    public void setMeta(String meta) {
        this.meta = meta;
    }
>>>>>>> 82aa1f0 (Initial commit)
}
