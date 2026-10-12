package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_in_company")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedPersonPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private RelatedPersonPostCode code;

    @Column(name = "code_in_company_registration_system", nullable = false, unique = true, length = 64)
    private String codeInCompanyRegistrationSystem;

    @Column(name="title", nullable=false) private String title;

    @PrePersist
    @PreUpdate
    private void normalizeCode() {
        if (codeInCompanyRegistrationSystem == null && code != null) {
            codeInCompanyRegistrationSystem = code.companyRegistrationCode();
        }
        if (codeInCompanyRegistrationSystem == null || codeInCompanyRegistrationSystem.isBlank()) {
            throw new IllegalStateException("کد سمت در سامانه ثبت شرکت‌ها الزامی است");
        }
    }

    @PostLoad
    private void resolveInternalCode() {
        try {
            code = RelatedPersonPostCode.fromCompanyRegistrationCode(codeInCompanyRegistrationSystem);
        } catch (IllegalArgumentException ignored) {
            code = null;
        }
    }
}
