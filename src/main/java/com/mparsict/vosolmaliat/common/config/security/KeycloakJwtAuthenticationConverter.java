package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.application.security.KeycloakPrincipalMapper;
import com.mparsict.vosolmaliat.domain.repository.security.GroupPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakPrincipalMapper principalMapper;
    private final GroupPermissionRepository groupPermissionRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> identityGroups = principalMapper.extractIdentityGroups(jwt);
        Set<String> authorities = new TreeSet<>(identityGroups);
        authorities.addAll(identityGroups.stream().map(role -> "ROLE_" + role).collect(Collectors.toSet()));
        authorities.addAll(groupPermissionRepository.findByGroupNameIn(identityGroups).stream()
                .filter(groupPermission -> Boolean.TRUE.equals(groupPermission.getPermission().getEnabled()))
                .map(groupPermission -> groupPermission.getPermission().getCode())
                .collect(Collectors.toSet()));

        Set<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new JwtAuthenticationToken(jwt, grantedAuthorities, principalMapper.username(jwt));
    }
}
