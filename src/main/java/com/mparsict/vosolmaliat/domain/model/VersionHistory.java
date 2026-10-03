package com.mparsict.vosolmaliat.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter @Setter
public abstract class VersionHistory extends BaseEntity {
    @Column(name = "next_version_creation_date", nullable = false, updatable = false)
    private LocalDateTime nextVersionCreationDate;

    public void captureAudit(BaseEntity source) {
        setCreatorUserId(source.getCreatorUserId());
        setUpdaterUserId(source.getUpdaterUserId());
        setCreatedAt(source.getCreatedAt());
        setUpdatedAt(source.getUpdatedAt());
        setCreatedBy(source.getCreatedBy());
        setUpdatedBy(source.getUpdatedBy());
        nextVersionCreationDate = LocalDateTime.now();
    }

    @PrePersist
    private void requireTransferDate() {
        if (nextVersionCreationDate == null) nextVersionCreationDate = LocalDateTime.now();
    }
}
