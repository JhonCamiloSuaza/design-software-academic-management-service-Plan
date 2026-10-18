package co.sena.academic.adapter.in.rest.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Local-only decoder; production profiles must validate JWTs against IAM JWKS. */
@Configuration
@Profile("dev")
public class AcademicDevSecurityConfig {

    @Bean
    JwtDecoder devJwtDecoder() {
        UUID devUser = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(devUser.toString())
                .claim("features", List.of(
                        "ACADEMIC_FICHA_VIEW:GLOBAL",
                        "ACADEMIC_FICHA_MANAGE:GLOBAL"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
