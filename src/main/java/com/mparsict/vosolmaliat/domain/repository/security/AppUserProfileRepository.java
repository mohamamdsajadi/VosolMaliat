package com.mparsict.vosolmaliat.domain.repository.security;

import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AppUserProfileRepository extends JpaRepository<AppUserProfile, Long> {
    Optional<AppUserProfile> findByKeycloakUserId(String keycloakUserId);
    Optional<AppUserProfile> findByUsername(String username);
    List<AppUserProfile> findByBusinessRole(BusinessRole businessRole);
    boolean existsByKeycloakUserId(String keycloakUserId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
