package co.sena.academic.api;

import co.sena.academic.adapter.out.event.AcademicOutboxWriter;
import co.sena.academic.adapter.out.event.AcademicOutboxJpaRepository;
import co.sena.academic.adapter.out.event.OutboxCompetencyEventPublisher;
import co.sena.academic.adapter.out.event.OutboxFichaEventPublisher;
import co.sena.academic.adapter.out.event.OutboxTrainingProgramEventPublisher;
import co.sena.academic.adapter.out.reference.ReferenceDataTrainingCenterAdapter;
import co.sena.academic.adapter.out.actors.ActorsFichaOwnershipAdapter;
import co.sena.academic.adapter.out.actors.IamClientCredentialsTokenProvider;
import co.sena.academic.adapter.out.actors.ServiceAccessTokenProvider;
import co.sena.academic.application.port.out.CompetencyEventPublisherPort;
import co.sena.academic.application.port.out.CompetencyRepositoryPort;
import co.sena.academic.application.port.out.CompetencyUsagePort;
import co.sena.academic.application.port.out.FichaEventPublisherPort;
import co.sena.academic.application.port.out.FichaRepositoryPort;
import co.sena.academic.application.port.out.FichaReportRepositoryPort;
import co.sena.academic.application.port.out.FichaOwnershipPort;
import co.sena.academic.application.port.out.KnowledgeNetworkRepositoryPort;
import co.sena.academic.application.port.out.TechLineLookupPort;
import co.sena.academic.application.port.out.TechLineRepositoryPort;
import co.sena.academic.application.port.out.TechNetworkLookupPort;
import co.sena.academic.application.port.out.TechNetworkRepositoryPort;
import co.sena.academic.application.port.out.TrainingCenterLookupPort;
import co.sena.academic.application.port.out.TrainingProgramEventPublisherPort;
import co.sena.academic.application.port.out.TrainingProgramLookupPort;
import co.sena.academic.application.port.out.TrainingProgramRepositoryPort;
import co.sena.academic.application.port.out.LearningOutcomeRepositoryPort;
import co.sena.academic.application.usecase.CompetencyService;
import co.sena.academic.application.usecase.FichaService;
import co.sena.academic.application.usecase.FichaReportService;
import co.sena.academic.application.usecase.KnowledgeNetworkService;
import co.sena.academic.application.usecase.TechLineService;
import co.sena.academic.application.usecase.TechNetworkService;
import co.sena.academic.application.usecase.TrainingProgramService;
import co.sena.academic.application.usecase.LearningOutcomeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@ComponentScan(basePackages = "co.sena.academic")
@EntityScan(basePackages = {"co.sena.academic.adapter.out.persistence", "co.sena.academic.adapter.out.event"})
@EnableJpaRepositories(basePackages = {"co.sena.academic.adapter.out.persistence", "co.sena.academic.adapter.out.event"})
@EnableScheduling
public class AcademicManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicManagementApiApplication.class, args);
    }

    @Bean
    public TechLineService techLineService(TechLineRepositoryPort techLineRepositoryPort) {
        return new TechLineService(techLineRepositoryPort);
    }

    @Bean
    public TechNetworkService techNetworkService(
            TechNetworkRepositoryPort repositoryPort,
            TechLineLookupPort techLineLookupPort) {
        return new TechNetworkService(repositoryPort, techLineLookupPort);
    }

    @Bean
    public KnowledgeNetworkService knowledgeNetworkService(
            KnowledgeNetworkRepositoryPort knowledgeNetworkRepositoryPort,
            TechNetworkLookupPort techNetworkLookupPort,
            TrainingProgramLookupPort trainingProgramLookupPort) {
        return new KnowledgeNetworkService(
                knowledgeNetworkRepositoryPort,
                techNetworkLookupPort,
                trainingProgramLookupPort);
    }

    @Bean
    public TrainingProgramService trainingProgramService(
            TrainingProgramRepositoryPort trainingProgramRepositoryPort,
            KnowledgeNetworkRepositoryPort knowledgeNetworkRepositoryPort,
            TrainingProgramEventPublisherPort trainingProgramEventPublisherPort) {
        return new TrainingProgramService(
                trainingProgramRepositoryPort,
                knowledgeNetworkRepositoryPort,
                trainingProgramEventPublisherPort);
    }

    @Bean
    public TrainingProgramEventPublisherPort trainingProgramEventPublisherPort(
            AcademicOutboxWriter academicOutboxWriter) {
        return new OutboxTrainingProgramEventPublisher(academicOutboxWriter);
    }

    @Bean
    public AcademicOutboxWriter academicOutboxWriter(
            AcademicOutboxJpaRepository academicOutboxJpaRepository,
            ObjectMapper objectMapper) {
        return new AcademicOutboxWriter(academicOutboxJpaRepository, objectMapper);
    }

    @Bean
    public RestClient referenceDataRestClient(
            RestClient.Builder builder,
            @Value("${reference-data.base-url:http://localhost:8002/api/v1}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public TrainingCenterLookupPort trainingCenterLookupPort(RestClient referenceDataRestClient) {
        return new ReferenceDataTrainingCenterAdapter(referenceDataRestClient);
    }

    @Bean
    @Qualifier("actorsRestClient")
    public RestClient actorsRestClient(
            RestClient.Builder builder,
            @Value("${actors-service.base-url:http://localhost:8004/api/v1}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("iamTokenRestClient")
    public RestClient iamTokenRestClient(
            RestClient.Builder builder,
            @Value("${iam.base-url:http://localhost:8080/api/v1}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public ServiceAccessTokenProvider actorsServiceAccessTokenProvider(
            @Qualifier("iamTokenRestClient") RestClient iamTokenRestClient,
            @Value("${iam.m2m.academic-client-id:academic-management-service}") String clientId,
            @Value("${iam.m2m.academic-client-secret:}") String clientSecret) {
        return new IamClientCredentialsTokenProvider(
                iamTokenRestClient, clientId, clientSecret, java.time.Clock.systemUTC());
    }

    @Bean
    public FichaOwnershipPort fichaOwnershipPort(
            @Qualifier("actorsRestClient") RestClient actorsRestClient,
            ServiceAccessTokenProvider serviceAccessTokenProvider) {
        return new ActorsFichaOwnershipAdapter(actorsRestClient, serviceAccessTokenProvider);
    }

    @Bean
    public FichaService fichaService(
            FichaRepositoryPort fichaRepositoryPort,
            TrainingProgramRepositoryPort trainingProgramRepositoryPort,
            TrainingCenterLookupPort trainingCenterLookupPort,
            FichaEventPublisherPort fichaEventPublisherPort,
            FichaOwnershipPort fichaOwnershipPort) {
        return new FichaService(
                fichaRepositoryPort,
                trainingProgramRepositoryPort,
                trainingCenterLookupPort,
                fichaEventPublisherPort,
                fichaOwnershipPort);
    }

    @Bean
    public FichaReportService fichaReportService(FichaReportRepositoryPort fichaReportRepositoryPort) {
        return new FichaReportService(fichaReportRepositoryPort);
    }

    @Bean
    public FichaEventPublisherPort fichaEventPublisherPort(
            AcademicOutboxWriter academicOutboxWriter) {
        return new OutboxFichaEventPublisher(academicOutboxWriter);
    }

    @Bean
    public CompetencyEventPublisherPort competencyEventPublisherPort(
            AcademicOutboxWriter academicOutboxWriter) {
        return new OutboxCompetencyEventPublisher(academicOutboxWriter);
    }

    @Bean
    public CompetencyService competencyService(
            CompetencyRepositoryPort competencyRepositoryPort,
            TrainingProgramRepositoryPort trainingProgramRepositoryPort,
            CompetencyUsagePort competencyUsagePort,
            CompetencyEventPublisherPort competencyEventPublisherPort) {
        return new CompetencyService(
                competencyRepositoryPort,
                trainingProgramRepositoryPort,
                competencyUsagePort,
                competencyEventPublisherPort);
    }

    @Bean
    public LearningOutcomeService learningOutcomeService(
            LearningOutcomeRepositoryPort learningOutcomeRepositoryPort,
            CompetencyRepositoryPort competencyRepositoryPort) {
        return new LearningOutcomeService(learningOutcomeRepositoryPort, competencyRepositoryPort);
    }
}
