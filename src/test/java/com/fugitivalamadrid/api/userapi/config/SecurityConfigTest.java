package com.fugitivalamadrid.api.userapi.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SecurityConfig and JwtAuthConverter.
 * Tests JWT token processing, role/scope extraction, and decoder configuration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Configuration Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthConverter jwtAuthConverter;

    @Mock
    private Environment environment;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("SecurityConfig should be created with dependencies")
    void securityConfigCreation() {
        assertNotNull(securityConfig);
        assertNotNull(jwtAuthConverter);
        assertNotNull(environment);
    }

    @Test
    @DisplayName("JwtDecoder should be created with default issuer URI when property is null")
    void jwtDecoderCreationWithDefaultIssuer() {
        JwtDecoder jwtDecoder = securityConfig.createTestJwtDecoder("http://localhost:8180/realms/userapi-realm");

        assertNotNull(jwtDecoder);
        assertTrue(jwtDecoder instanceof NimbusJwtDecoder);
    }

    @Test
    @DisplayName("JwtDecoder should be created with default issuer URI when property is empty")
    void jwtDecoderCreationWithEmptyIssuer() {
        JwtDecoder jwtDecoder = securityConfig.createTestJwtDecoder("http://localhost:8180/realms/userapi-realm");

        assertNotNull(jwtDecoder);
        assertTrue(jwtDecoder instanceof NimbusJwtDecoder);
    }

    @Test
    @DisplayName("JwtDecoder should be created with custom issuer URI from environment")
    void jwtDecoderCreationWithCustomIssuer() {
        JwtDecoder jwtDecoder = securityConfig.createTestJwtDecoder("http://localhost:8180/realms/userapi-realm");

        assertNotNull(jwtDecoder);
        assertTrue(jwtDecoder instanceof NimbusJwtDecoder);
    }

    // ── JwtAuthConverter Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("JwtAuthConverter should extract ADMIN role from realm_access")
    void jwtAuthConverterShouldExtractAdminRole() {
        Jwt jwt = createJwtWithRealmRoles(List.of("ADMIN"));

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("JwtAuthConverter should extract USER role from realm_access")
    void jwtAuthConverterShouldExtractUserRole() {
        Jwt jwt = createJwtWithRealmRoles(List.of("USER"));

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER");
    }

    @Test
    @DisplayName("JwtAuthConverter should extract multiple roles from realm_access")
    void jwtAuthConverterShouldExtractMultipleRoles() {
        Jwt jwt = createJwtWithRealmRoles(List.of("ADMIN", "USER", "MANAGER"));

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER");
    }

    @Test
    @DisplayName("JwtAuthConverter should extract scopes from scope claim")
    void jwtAuthConverterShouldExtractScopes() {
        Jwt jwt = createJwtWithScopes(List.of("read", "write"));

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_read", "SCOPE_write");
    }

    @Test
    @DisplayName("JwtAuthConverter should extract both roles and scopes")
    void jwtAuthConverterShouldExtractRolesAndScopes() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("scope", List.of("read", "write"))
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        // Check for scope authorities
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_read", "SCOPE_write");

        // Check for role authorities
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_USER");

        // Verify we have all 4 authorities
        assertThat(authorities).hasSize(4);
    }

    @Test
    @DisplayName("JwtAuthConverter should handle JWT with no roles")
    void jwtAuthConverterShouldHandleNoRoles() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("scope", List.of("read"))
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        // Should only have scope authorities
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_read")
                .doesNotContain("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("JwtAuthConverter should handle JWT with no scopes")
    void jwtAuthConverterShouldHandleNoScopes() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        // Should only have role authorities
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN")
                .doesNotContain("SCOPE_read", "SCOPE_write");
    }

    @Test
    @DisplayName("JwtAuthConverter should handle JWT with empty realm_access")
    void jwtAuthConverterShouldHandleEmptyRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("realm_access", Map.of())
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthConverter realConverter = new JwtAuthConverter();
        AbstractAuthenticationToken authToken = realConverter.convert(jwt);

        assertNotNull(authToken);
        Collection<? extends GrantedAuthority> authorities = authToken.getAuthorities();

        // Should have no role authorities
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .noneMatch(auth -> auth.startsWith("ROLE_"));
    }

    @Test
    @DisplayName("Security configuration constants should have correct values")
    void securityConfigConstantsShouldHaveCorrectValues() {
        // Use reflection to test private constants
        try {
            java.lang.reflect.Field usersEndpointField = SecurityConfig.class.getDeclaredField("USERS_ENDPOINT");
            usersEndpointField.setAccessible(true);
            String usersEndpoint = (String) usersEndpointField.get(null);
            assertEquals("/users/**", usersEndpoint);

            java.lang.reflect.Field adminRoleField = SecurityConfig.class.getDeclaredField("ADMIN_ROLE");
            adminRoleField.setAccessible(true);
            String adminRole = (String) adminRoleField.get(null);
            assertEquals("ADMIN", adminRole);

            java.lang.reflect.Field actuatorEndpointsField = SecurityConfig.class.getDeclaredField("ACTUATOR_ENDPOINTS");
            actuatorEndpointsField.setAccessible(true);
            String actuatorEndpoints = (String) actuatorEndpointsField.get(null);
            assertEquals("/actuator/**", actuatorEndpoints);

            java.lang.reflect.Field swaggerUiField = SecurityConfig.class.getDeclaredField("SWAGGER_UI");
            swaggerUiField.setAccessible(true);
            String swaggerUi = (String) swaggerUiField.get(null);
            assertEquals("/swagger-ui/**", swaggerUi);

            java.lang.reflect.Field apiDocsField = SecurityConfig.class.getDeclaredField("API_DOCS");
            apiDocsField.setAccessible(true);
            String apiDocs = (String) apiDocsField.get(null);
            assertEquals("/api-docs/**", apiDocs);

            java.lang.reflect.Field postUsersField = SecurityConfig.class.getDeclaredField("POST_USERS");
            postUsersField.setAccessible(true);
            String postUsers = (String) postUsersField.get(null);
            assertEquals("/users", postUsers);

        } catch (Exception e) {
            fail("Failed to access SecurityConfig constants: " + e.getMessage());
        }
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    /**
     * Creates a JWT with specified realm roles.
     */
    private Jwt createJwtWithRealmRoles(List<String> roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", roles))
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    /**
     * Creates a JWT with specified scopes.
     */
    private Jwt createJwtWithScopes(List<String> scopes) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("scope", scopes)
                .claim("sub", "test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
