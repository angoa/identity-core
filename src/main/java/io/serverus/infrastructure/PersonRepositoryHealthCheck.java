package io.serverus.infrastructure.health;

import io.serverus.domain.port.out.PersonRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check de disponibilidad del repositorio de personas.
 *
 * Verifica que el repositorio esta operativo y puede atender
 * peticiones. OpenShift usa este check para determinar si el
 * pod puede recibir trafico.
 *
 * @since 1.0.0
 */
@Readiness
@ApplicationScoped
public class PersonRepositoryHealthCheck implements HealthCheck {

    @Inject
    PersonRepository personRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            personRepository.existsByCurp(
                    io.serverus.domain.model.vo.Curp.of("MOAA800101HDFRRN09")
            );
            return HealthCheckResponse.named("identity-core/person-repository")
                    .up()
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("identity-core/person-repository")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
