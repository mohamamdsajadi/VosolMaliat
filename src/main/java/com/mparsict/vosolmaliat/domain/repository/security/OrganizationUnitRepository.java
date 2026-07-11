package com.mparsict.vosolmaliat.domain.repository.security;

import com.mparsict.vosolmaliat.domain.model.security.OrganizationUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long> {
    Optional<OrganizationUnit> findByCode(String code);
    boolean existsByCode(String code);
}
