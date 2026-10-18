package co.sena.academic.adapter.out.actors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.sena.academic.domain.exception.FichaOwnershipUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ActorsFichaOwnershipAdapterTest {

    @Test
    void readsInstructorFichaIdsFromActorsInternalApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://actors.test/api/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID actorId = UUID.randomUUID();
        UUID fichaId = UUID.randomUUID();
        server.expect(requestTo("http://actors.test/api/v1/internal/instructors/"
                        + actorId + "/fichas"))
                .andRespond(withSuccess(
                        "{\"ficha_ids\":[\"" + fichaId + "\"]}",
                        MediaType.APPLICATION_JSON));

        SetAssertions.assertContainsExactly(
                adapter(builder.build()).findFichaIdsForInstructor(actorId), fichaId);
        server.verify();
    }

    @Test
    void readsActiveLearnerCountFromActorsInternalApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://actors.test/api/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID fichaId = UUID.randomUUID();
        server.expect(requestTo("http://actors.test/api/v1/internal/fichas/"
                        + fichaId + "/active-learners/count"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-access-token"))
                .andRespond(withSuccess(
                        "{\"active_learner_count\":3}",
                        MediaType.APPLICATION_JSON));

        assertThat(adapter(builder.build())
                .countActiveLearners(fichaId)).isEqualTo(3);
        server.verify();
    }

    @Test
    void failsClosedWhenIamCannotIssueServiceToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://actors.test/api/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new ActorsFichaOwnershipAdapter(builder.build(), () -> {
            throw new ServiceAccessTokenUnavailableException("IAM unavailable");
        });

        assertThatThrownBy(() -> adapter.countActiveLearners(UUID.randomUUID()))
                .isInstanceOf(FichaOwnershipUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsMalformedCountResponseInsteadOfTreatingItAsZero() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://actors.test/api/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UUID fichaId = UUID.randomUUID();
        server.expect(requestTo("http://actors.test/api/v1/internal/fichas/"
                        + fichaId + "/active-learners/count"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter(builder.build()).countActiveLearners(fichaId))
                .isInstanceOf(FichaOwnershipUnavailableException.class);
        server.verify();
    }

    private static ActorsFichaOwnershipAdapter adapter(RestClient restClient) {
        return new ActorsFichaOwnershipAdapter(restClient, () -> "service-access-token");
    }

    private static final class SetAssertions {
        private static void assertContainsExactly(java.util.Set<UUID> actual, UUID expected) {
            assertThat(actual).containsExactly(expected);
        }
    }
}
