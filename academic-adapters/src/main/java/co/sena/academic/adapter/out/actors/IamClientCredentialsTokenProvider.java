package co.sena.academic.adapter.out.actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Obtains and caches IAM client-credentials tokens until shortly before their expiry. */
public class IamClientCredentialsTokenProvider implements ServiceAccessTokenProvider {

    private final RestClient iamClient;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;
    private volatile CachedToken cachedToken;

    public IamClientCredentialsTokenProvider(
            RestClient iamClient, String clientId, String clientSecret, Clock clock) {
        this.iamClient = iamClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    @Override
    public String accessToken() {
        CachedToken current = cachedToken;
        if (current != null && clock.instant().isBefore(current.refreshAt())) {
            return current.value();
        }
        synchronized (this) {
            current = cachedToken;
            if (current != null && clock.instant().isBefore(current.refreshAt())) {
                return current.value();
            }
            return fetchToken();
        }
    }

    private String fetchToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new ServiceAccessTokenUnavailableException("IAM M2M client credentials are not configured");
        }
        String basic = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        try {
            TokenResponse response = iamClient.post()
                    .uri("/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", "Basic " + basic)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                    || !"Bearer".equalsIgnoreCase(response.tokenType()) || response.expiresIn() <= 0) {
                throw new ServiceAccessTokenUnavailableException("IAM returned an invalid M2M token response");
            }
            Instant now = clock.instant();
            long refreshBufferSeconds = Math.min(30, Math.max(1, response.expiresIn() / 5));
            cachedToken = new CachedToken(response.accessToken(),
                    now.plusSeconds(Math.max(1, response.expiresIn() - refreshBufferSeconds)));
            return response.accessToken();
        } catch (RestClientException ex) {
            throw new ServiceAccessTokenUnavailableException("IAM token endpoint is unavailable", ex);
        }
    }

    private record CachedToken(String value, Instant refreshAt) {
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("scope") String scope) {
    }
}
