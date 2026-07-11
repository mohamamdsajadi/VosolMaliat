package com.mparsict.vosolmaliat.domain.repository.security;

import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupPermissionRepository extends JpaRepository<GroupPermission, Long> {
    @EntityGraph(attributePaths = "permission")
    List<GroupPermission> findByGroupNameIn(Collection<String> groupNames);

    @EntityGraph(attributePaths = "permission")
    List<GroupPermission> findByGroupName(String groupName);

    Optional<GroupPermission> findByGroupNameAndPermissionCode(String groupName, String permissionCode);
}
