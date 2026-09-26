package com.toollix.analytics.model;

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

    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    public AnalyticsEvent() {}

    public Long getId() { return id; }
    public Long getProfileId() { return profileId; }
    public Long getNfcCardId() { return nfcCardId; }
    public String getEventType() { return eventType; }
    public String getMeta() { return meta; }
}
