package com.fugitivalamadrid.api.userapi.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Converts a JWT into an AbstractAuthenticationToken.
     * @param jwt the JWT to convert
     * @return an AbstractAuthenticationToken
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(extractScopeAuthorities(jwt));
        authorities.addAll(extractRealmRoleAuthorities(jwt));
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * Extracts scope authorities from the JWT.
     * @param jwt the JWT to extract scope authorities from
     * @return a collection of scope authorities
     */
    private Collection<? extends GrantedAuthority> extractScopeAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new HashSet<>();
        List<String> scopes = jwt.getClaimAsStringList("scope");
        if (scopes != null) {
            for (String scope : scopes) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            }
        }
        return authorities;
    }

    /**
     * Extracts realm role authorities from the JWT.
     * @param jwt the JWT to extract realm role authorities from
     * @return a collection of realm role authorities
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractRealmRoleAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new HashSet<>();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        return authorities;
    }
}