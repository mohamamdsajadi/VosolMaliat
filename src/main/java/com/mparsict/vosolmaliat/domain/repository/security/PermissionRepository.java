package com.mparsict.vosolmaliat.domain.repository.security;

import com.mparsict.vosolmaliat.domain.model.security.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    boolean existsByCode(String code);
    List<Permission> findByCodeInAndEnabledTrue(Collection<String> codes);
}
