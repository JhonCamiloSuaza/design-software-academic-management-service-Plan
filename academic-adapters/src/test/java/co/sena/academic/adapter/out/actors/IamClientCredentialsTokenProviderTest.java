package co.sena.academic.adapter.out.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class IamClientCredentialsTokenProviderTest {

    @Test
    void obtainsAndCachesShortLivedIamTokenUsingBasicClientCredentials() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://iam.test/api/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String basic = "Basic " + Base64.getEncoder().encodeToString(
                "academic-management-service:local-test-secret".getBytes(StandardCharsets.UTF_8));
        server.expect(requestTo("http://iam.test/api/v1/auth/token"))
                .andExpect(header("Authorization", basic))
                .andExpect(content().string("grant_type=client_credentials"))
                .andRespond(withSuccess("""
                        {"access_token":"signed-m2m-token","token_type":"Bearer","expires_in":300,
                         "scope":"ACT_INTERNAL_FICHA_LEARNER_COUNT:GLOBAL ACADEMIC_FICHA_VIEW:GLOBAL"}
                        """, MediaType.APPLICATION_JSON));

        var provider = new IamClientCredentialsTokenProvider(
                builder.build(), "academic-management-service", "local-test-secret",
                Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC));

        assertThat(provider.accessToken()).isEqualTo("signed-m2m-token");
        assertThat(provider.accessToken()).isEqualTo("signed-m2m-token");
        server.verify();
    }

    @Test
    void failsClosedWhenCredentialsAreMissing() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://iam.test/api/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var provider = new IamClientCredentialsTokenProvider(
                builder.build(), "academic-management-service", "", Clock.systemUTC());

        assertThatThrownBy(provider::accessToken)
                .isInstanceOf(ServiceAccessTokenUnavailableException.class);
        server.verify();
    }
}
