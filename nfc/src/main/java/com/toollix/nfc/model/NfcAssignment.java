package com.toollix.nfc.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nfc_assignments")
public class NfcAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "nfc_card_id")
    private NfcCard nfcCard;

    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "assigned_at")
    private Instant assignedAt = Instant.now();

    @Column(name = "unassigned_at")
    private Instant unassignedAt;

    public NfcAssignment() {}

    public Long getId() { return id; }
    public NfcCard getNfcCard() { return nfcCard; }
    public void setNfcCard(NfcCard nfcCard) { this.nfcCard = nfcCard; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getUnassignedAt() { return unassignedAt; }
    public void setUnassignedAt(Instant unassignedAt) { this.unassignedAt = unassignedAt; }
}
