package co.sena.academic.adapter.in.rest.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates the real iam-service JWT (claim "features": ["FEATURE_CODE:SCOPE_TYPE", ...])
 * into GrantedAuthority instances (uses only FEATURE_CODE, before the ":") and exposes the
 * actor ("sub") as a UUID in the principal, so that @AuthenticationPrincipal UUID works
 * directly in the controllers, as TechLineController already assumes.
 */
@Component
public class JwtFeaturesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> features = jwt.getClaimAsStringList("features");
        List<GrantedAuthority> authorities = (features == null ? List.<String>of() : features).stream()
                .map(f -> f.split(":", 2)[0])
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new AcademicJwtAuthenticationToken(jwt, authorities);
    }
}
