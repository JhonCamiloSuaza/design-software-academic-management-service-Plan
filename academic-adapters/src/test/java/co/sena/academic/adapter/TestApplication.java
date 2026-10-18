package co.sena.academic.adapter;

import co.sena.academic.application.port.out.CompetencyEventPublisherPort;
import co.sena.academic.application.port.out.CompetencyRepositoryPort;
import co.sena.academic.application.port.out.CompetencyUsagePort;
import co.sena.academic.application.port.out.FichaEventPublisherPort;
import co.sena.academic.application.port.out.FichaOwnershipPort;
import co.sena.academic.application.port.out.FichaRepositoryPort;
import co.sena.academic.application.port.out.FichaReportRepositoryPort;
import co.sena.academic.application.port.out.KnowledgeNetworkRepositoryPort;
import co.sena.academic.application.port.out.LearningOutcomeRepositoryPort;
import co.sena.academic.application.port.out.TechLineLookupPort;
import co.sena.academic.application.port.out.TechLineRepositoryPort;
import co.sena.academic.application.port.out.TechNetworkLookupPort;
import co.sena.academic.application.port.out.TechNetworkRepositoryPort;
import co.sena.academic.application.port.out.TrainingCenterLookupPort;
import co.sena.academic.application.port.out.TrainingProgramEventPublisherPort;
import co.sena.academic.application.port.out.TrainingProgramLookupPort;
import co.sena.academic.application.port.out.TrainingProgramRepositoryPort;
import co.sena.academic.application.usecase.CompetencyService;
import co.sena.academic.application.usecase.FichaService;
import co.sena.academic.application.usecase.FichaReportService;
import co.sena.academic.application.usecase.KnowledgeNetworkService;
import co.sena.academic.application.usecase.LearningOutcomeService;
import co.sena.academic.application.usecase.TechLineService;
import co.sena.academic.application.usecase.TechNetworkService;
import co.sena.academic.application.usecase.TrainingProgramService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = RabbitAutoConfiguration.class)
@ComponentScan(basePackages = "co.sena.academic")
@EntityScan(basePackages = "co.sena.academic.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "co.sena.academic.adapter.out.persistence")
public class TestApplication {

    @Bean
    public TechLineService techLineService(TechLineRepositoryPort techLineRepositoryPort) {
        return new TechLineService(techLineRepositoryPort);
    }

    @Bean
    public TechNetworkService techNetworkService(TechNetworkRepositoryPort repositoryPort,
                                                 TechLineLookupPort techLineLookupPort) {
        return new TechNetworkService(repositoryPort, techLineLookupPort);
    }

    @Bean
    public KnowledgeNetworkService knowledgeNetworkService(
            KnowledgeNetworkRepositoryPort repositoryPort,
            TechNetworkLookupPort techNetworkLookupPort,
            TrainingProgramLookupPort trainingProgramLookupPort) {
        return new KnowledgeNetworkService(repositoryPort, techNetworkLookupPort, trainingProgramLookupPort);
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

    /** HU-ACAD-08 ficha registration with explicit external-service doubles. */
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
    @Primary
    public RecordingFichaEventPublisher recordingFichaEventPublisher() {
        return new RecordingFichaEventPublisher();
    }

    @Bean
    @Primary
    public RecordingTrainingCenterLookup recordingTrainingCenterLookup() {
        return new RecordingTrainingCenterLookup();
    }

    @Bean
    @Primary
    public RecordingFichaOwnershipPort recordingFichaOwnershipPort() {
        return new RecordingFichaOwnershipPort();
    }

    @Bean
    @Primary
    public RecordingCompetencyEventPublisher recordingCompetencyEventPublisher() {
        return new RecordingCompetencyEventPublisher();
    }

    @Bean
    @Primary
    public RecordingTrainingProgramEventPublisher recordingTrainingProgramEventPublisher() {
        return new RecordingTrainingProgramEventPublisher();
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
