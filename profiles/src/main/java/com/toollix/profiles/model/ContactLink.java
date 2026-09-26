package com.toollix.profiles.model;

import jakarta.persistence.*;

@Entity
@Table(name = "contact_links")
public class ContactLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "type", nullable = false, length = 50)
    private String type; // whatsapp | phone | email | website | linkedin | twitter | instagram | github

    @Column(name = "title")
    private String title;

    @Column(name = "url")
    private String url;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "visible")
    private boolean visible = true;

    public ContactLink() {
    }

    public Long getId() {
        return id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
