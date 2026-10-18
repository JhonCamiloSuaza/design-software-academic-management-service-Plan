package co.sena.academic.adapter.in.rest.security;

import co.sena.academic.application.port.in.QueryFichasUseCase;
import co.sena.academic.domain.exception.FichaScopeAccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class FichaAccessContextResolver {

    public QueryFichasUseCase.AccessContext resolve(Authentication authentication) {
        Jwt jwt = extractJwt(authentication);
        if (jwt == null) {
            throw new FichaScopeAccessDeniedException("JWT context is required");
        }

        UUID userId = requiredUuid(jwt.getSubject(), "sub");
        UUID actorId = optionalUuid(jwt.getClaimAsString("actor_id"), "actor_id");
        UUID trainingCenterId = optionalUuid(
                jwt.getClaimAsString("training_center_id"), "training_center_id");
        QueryFichasUseCase.ScopeType scope = resolveScope(
                jwt.getClaimAsStringList("features"));

        if (scope == QueryFichasUseCase.ScopeType.TRAINING_CENTER
                && trainingCenterId == null) {
            throw new FichaScopeAccessDeniedException(
                    "training_center_id is required for TRAINING_CENTER");
        }
        if ((scope == QueryFichasUseCase.ScopeType.OWN_FICHAS
                || scope == QueryFichasUseCase.ScopeType.OWN_FICHA_AS_LEARNER)
                && actorId == null) {
            throw new FichaScopeAccessDeniedException("actor_id is required for own scope");
        }

        return new QueryFichasUseCase.AccessContext(userId, actorId, trainingCenterId, scope);
    }

    /** Resolves a management scope from the exact management feature claim. */
    public QueryFichasUseCase.AccessContext resolve(Authentication authentication, String featureCode) {
        Jwt jwt = extractJwt(authentication);
        if (jwt == null) {
            throw new FichaScopeAccessDeniedException("JWT context is required");
        }

        UUID userId = requiredUuid(jwt.getSubject(), "sub");
        UUID actorId = optionalUuid(jwt.getClaimAsString("actor_id"), "actor_id");
        UUID trainingCenterId = optionalUuid(
                jwt.getClaimAsString("training_center_id"), "training_center_id");
        QueryFichasUseCase.ScopeType scope = resolveManageScope(
                jwt.getClaimAsStringList("features"), featureCode);

        if (scope == QueryFichasUseCase.ScopeType.TRAINING_CENTER
                && trainingCenterId == null) {
            throw new FichaScopeAccessDeniedException(
                    "training_center_id is required for TRAINING_CENTER");
        }

        return new QueryFichasUseCase.AccessContext(userId, actorId, trainingCenterId, scope);
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken();
        }
        if (authentication != null && authentication.getCredentials() instanceof Jwt jwt) {
            return jwt;
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    private QueryFichasUseCase.ScopeType resolveScope(List<String> features) {
        QueryFichasUseCase.ScopeType selected = null;
        int selectedRank = -1;
        if (features != null) {
            for (String feature : features) {
                String[] parts = feature.split(":", 2);
                if (parts.length != 2
                        || (!"ACADEMIC_FICHA_VIEW".equals(parts[0])
                        && !"ACADEMIC_FICHA_VIEW_OWN".equals(parts[0]))) {
                    continue;
                }
                QueryFichasUseCase.ScopeType candidate = parseScope(parts[1]);
                int rank = rank(candidate);
                if (rank > selectedRank) {
                    selected = candidate;
                    selectedRank = rank;
                }
            }
        }
        if (selected == null) {
            throw new FichaScopeAccessDeniedException(
                    "A ficha feature with a supported scope is required");
        }
        return selected;
    }

    private QueryFichasUseCase.ScopeType resolveManageScope(List<String> features,
                                                             String featureCode) {
        QueryFichasUseCase.ScopeType selected = null;
        int selectedRank = -1;
        if (features != null) {
            for (String feature : features) {
                String[] parts = feature.split(":", 2);
                if (parts.length != 2 || !featureCode.equals(parts[0])) {
                    continue;
                }
                QueryFichasUseCase.ScopeType candidate = parseScope(parts[1]);
                if (candidate != QueryFichasUseCase.ScopeType.GLOBAL
                        && candidate != QueryFichasUseCase.ScopeType.TRAINING_CENTER) {
                    throw new FichaScopeAccessDeniedException(
                            featureCode + " supports GLOBAL or TRAINING_CENTER only");
                }
                int rank = rank(candidate);
                if (rank > selectedRank) {
                    selected = candidate;
                    selectedRank = rank;
                }
            }
        }
        if (selected == null) {
            throw new FichaScopeAccessDeniedException(
                    "A ficha management feature with a supported scope is required");
        }
        return selected;
    }

    private QueryFichasUseCase.ScopeType parseScope(String rawScope) {
        try {
            return QueryFichasUseCase.ScopeType.valueOf(rawScope);
        } catch (IllegalArgumentException ex) {
            throw new FichaScopeAccessDeniedException("Unsupported ficha scope: " + rawScope);
        }
    }

    private int rank(QueryFichasUseCase.ScopeType scope) {
        return switch (scope) {
            case GLOBAL -> 4;
            case TRAINING_CENTER -> 3;
            case OWN_FICHAS, OWN_FICHA_AS_LEARNER -> 2;
        };
    }

    private UUID requiredUuid(String value, String claim) {
        UUID parsed = optionalUuid(value, claim);
        if (parsed == null) {
            throw new FichaScopeAccessDeniedException(claim + " is required");
        }
        return parsed;
    }

    private UUID optionalUuid(String value, String claim) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new FichaScopeAccessDeniedException(claim + " must be a UUID");
        }
    }
}
