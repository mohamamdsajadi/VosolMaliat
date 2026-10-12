package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPersonPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RelatedPersonPostRepository extends JpaRepository<RelatedPersonPost, Long> {

    Optional<RelatedPersonPost> findByCodeInCompanyRegistrationSystem(String code);

    default Optional<RelatedPersonPost> findByCode(RelatedPersonPostCode code) {
        return code == null || !code.hasCompanyRegistrationCode()
                ? Optional.empty()
                : findByCodeInCompanyRegistrationSystem(code.companyRegistrationCode());
    }
}
