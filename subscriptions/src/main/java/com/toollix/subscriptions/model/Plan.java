package com.toollix.subscriptions.model;

import jakarta.persistence.*;

@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "billing_cycle")
    private String billingCycle;

    @Column(name = "price_cents")
    private Long priceCents = 0L;

    @Column(name = "features", columnDefinition = "jsonb")
    private String features;

    public Plan() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public Long getPriceCents() { return priceCents; }
    public void setPriceCents(Long priceCents) { this.priceCents = priceCents; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
}
