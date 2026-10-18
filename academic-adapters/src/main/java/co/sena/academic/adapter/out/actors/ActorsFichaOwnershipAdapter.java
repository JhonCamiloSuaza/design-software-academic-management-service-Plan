package co.sena.academic.adapter.out.actors;

import co.sena.academic.application.port.out.FichaOwnershipPort;
import co.sena.academic.domain.exception.FichaOwnershipUnavailableException;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.observation.annotation.Observed;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** HTTP adapter for the internal Actors ownership read API. */
public class ActorsFichaOwnershipAdapter implements FichaOwnershipPort {

    private final RestClient restClient;
    private final ServiceAccessTokenProvider serviceAccessTokenProvider;

    public ActorsFichaOwnershipAdapter(
            RestClient restClient, ServiceAccessTokenProvider serviceAccessTokenProvider) {
        this.restClient = restClient;
        this.serviceAccessTokenProvider = serviceAccessTokenProvider;
    }

    @Override
    public Set<UUID> findFichaIdsForInstructor(UUID instructorId) {
        return get("/internal/instructors/{actorId}/fichas", instructorId, "instructor");
    }

    @Override
    public Set<UUID> findFichaIdsForLearner(UUID learnerId) {
        return get("/internal/learners/{actorId}/fichas", learnerId, "learner");
    }

    @Override
    @Observed(name = "actors.ficha.active_learner_count")
    public int countActiveLearners(UUID fichaId) {
        try {
            String token = serviceAccessTokenProvider.accessToken();
            if (token == null || token.isBlank()) {
                throw new ServiceAccessTokenUnavailableException("IAM returned an empty M2M access token");
            }
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("/internal/fichas/{fichaId}/active-learners/count", fichaId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            ActiveLearnerCountResponse body = request
                    .retrieve()
                    .body(ActiveLearnerCountResponse.class);
            if (body == null || body.activeLearnerCount() == null || body.activeLearnerCount() < 0) {
                throw new FichaOwnershipUnavailableException(
                        "Actors returned an invalid active learner count for ficha " + fichaId);
            }
            return body.activeLearnerCount();
        } catch (ServiceAccessTokenUnavailableException ex) {
            throw new FichaOwnershipUnavailableException("IAM service token unavailable", ex);
        } catch (FichaOwnershipUnavailableException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new FichaOwnershipUnavailableException(
                    "Actors ownership service unavailable for ficha " + fichaId, ex);
        }
    }

    private Set<UUID> get(String uri, UUID actorId, String actorType) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri(uri, actorId);
            String token = currentBearerToken();
            if (token != null) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            ResponseEntity<FichaIdsResponse> response = request
                    .retrieve()
                    .toEntity(FichaIdsResponse.class);
            FichaIdsResponse body = response.getBody();
            return body == null || body.fichaIds() == null
                    ? Set.of()
                    : new LinkedHashSet<>(body.fichaIds());
        } catch (HttpClientErrorException.NotFound ex) {
            // An actor removed from Actors cannot own any visible ficha.
            return Set.of();
        } catch (RestClientException ex) {
            throw new FichaOwnershipUnavailableException(
                    "Actors ownership service unavailable for " + actorType + " " + actorId, ex);
        }
    }

    private String currentBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object credentials = authentication.getCredentials();
        if (credentials instanceof AbstractOAuth2Token token) {
            return token.getTokenValue();
        }
        return credentials instanceof String value ? value : null;
    }

    public record FichaIdsResponse(@JsonProperty("ficha_ids") Set<UUID> fichaIds) {
    }

    public record ActiveLearnerCountResponse(
            @JsonProperty("active_learner_count") Integer activeLearnerCount) {
    }
}
