package com.toollix.nfc.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nfc_cards")
public class NfcCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "internal_code", unique = true, nullable = false)
    private String internalCode;

    @Column(name = "chip_uid")
    private String chipUid;

    @Column(name = "status")
    private String status;

    @Column(name = "manufactured_at")
    private Instant manufacturedAt;

    public NfcCard() {}
    public Long getId() { return id; }
    public String getInternalCode() { return internalCode; }
    public void setInternalCode(String internalCode) { this.internalCode = internalCode; }
    public String getChipUid() { return chipUid; }
    public void setChipUid(String chipUid) { this.chipUid = chipUid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
