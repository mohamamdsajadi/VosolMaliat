package com.mparsict.vosolmaliat.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@lombok.NoArgsConstructor
@lombok.experimental.SuperBuilder
public abstract class BaseEntity {
    @Column(name="creator_user_id", nullable=false, updatable=false)
    private Long creatorUserId;
    @Column(name="updater_user_id", nullable=false)
    private Long updaterUserId;
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="creator_user_id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.mparsict.vosolmaliat.domain.model.security.AppUserProfile creatorUser;
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name="updater_user_id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.mparsict.vosolmaliat.domain.model.security.AppUserProfile updaterUser;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (creatorUserId == null) creatorUserId = AuditContext.userId();
        if (updaterUserId == null) updaterUserId = creatorUserId;
        if (createdBy == null) createdBy = currentActor();
        if (updatedBy == null) updatedBy = createdBy;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updatedBy = currentActor();
        updaterUserId = AuditContext.userId();
    }

    public static String currentActor() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken
                ? "SYSTEM" : authentication.getName();
    }
}
