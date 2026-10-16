package co.sena.academic.adapter.in.rest.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Keeps the standard JWT token available after authentication while exposing
 * the actor id as the principal expected by the REST controllers.
 */
final class AcademicJwtAuthenticationToken extends JwtAuthenticationToken {

    private final UUID actorId;

    AcademicJwtAuthenticationToken(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities, jwt.getSubject());
        this.actorId = UUID.fromString(jwt.getSubject());
    }

    @Override
    public Object getPrincipal() {
        return actorId;
    }
}
