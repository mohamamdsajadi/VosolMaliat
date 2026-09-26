package com.mparsict.vosolmaliat.application.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class KeycloakPrincipalMapper {

    public Set<String> extractRealmRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map)) {
            return Set.of();
        }
        return stringSet(map.get("roles"));
    }

    public Set<String> extractClientRoles(Jwt jwt) {
        Object resourceAccess = jwt.getClaim("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> clients)) {
            return Set.of();
        }
        Set<String> roles = new TreeSet<>();
        for (Object client : clients.values()) {
            if (client instanceof Map<?, ?> clientMap) {
                roles.addAll(stringSet(clientMap.get("roles")));
            }
        }
        return roles;
    }

    public Set<String> extractGroups(Jwt jwt) {
        Set<String> groups = new TreeSet<>();
        for (String group : stringSet(jwt.getClaim("groups"))) {
            String normalized = group;
            int slash = normalized.lastIndexOf('/');
            if (slash >= 0 && slash < normalized.length() - 1) {
                normalized = normalized.substring(slash + 1);
            }
            groups.add(normalized);
        }
        return groups;
    }

    public Set<String> extractIdentityGroups(Jwt jwt) {
        Set<String> values = new TreeSet<>();
        values.addAll(extractRealmRoles(jwt));
        values.addAll(extractClientRoles(jwt));
        values.addAll(extractGroups(jwt));
        return values;
    }

    public String username(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred == null || preferred.isBlank() ? jwt.getSubject() : preferred;
    }

    private Set<String> stringSet(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }
        Set<String> result = new TreeSet<>();
        for (Object item : collection) {
            if (item != null && !item.toString().isBlank()) {
                result.add(item.toString());
            }
        }
        return result;
    }
}
