package co.sena.academic.adapter.out.reference;

import co.sena.academic.application.port.out.TrainingCenterLookupPort;
import co.sena.academic.domain.exception.ReferenceDataServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * Synchronous reference-data client required by HU-ACAD-08.
 *
 * The reference-data service is currently a separate dependency. Integration
 * tests replace this adapter with an explicit recording double until its
 * endpoint is deployed.
 */
public class ReferenceDataTrainingCenterAdapter implements TrainingCenterLookupPort {

    private final RestClient restClient;

    public ReferenceDataTrainingCenterAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<TrainingCenterReference> findById(UUID trainingCenterId) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("/training-centers/{id}", trainingCenterId);
            String token = currentBearerToken();
            if (token != null) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            ResponseEntity<TrainingCenterResponse> response = request
                    .retrieve()
                    .toEntity(TrainingCenterResponse.class);
            TrainingCenterResponse body = response.getBody();
            return body == null
                    ? Optional.empty()
                    : Optional.of(new TrainingCenterReference(body.active()));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new ReferenceDataServiceUnavailableException(trainingCenterId, ex);
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

    public record TrainingCenterResponse(
            UUID id,
            @JsonProperty("is_active") boolean active) {
    }
}
