package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.LookupValue;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class LookupDefinition {
    @Id
    private Integer id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String title;

    protected LookupDefinition(LookupValue value) {
        this.id = value.id();
        this.code = ((Enum<?>) value).name();
        this.title = value.label();
    }
}
