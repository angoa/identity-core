package io.serverus.domain.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.serverus.domain.exception.BusinessRuleException;
import io.serverus.domain.exception.ResourceNotFoundException;
import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.port.in.RegisterPersonUseCase;
import io.serverus.domain.port.out.PersonRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

/**
 * Servicio de dominio que implementa los casos de uso relacionados
 * con el ciclo de vida de una persona en el sistema de identidad digital.
 *
 * Orquesta la logica de negocio delegando la persistencia al repositorio
 * y garantizando que las reglas del dominio se cumplan antes de
 * cualquier operacion.
 *
 * @since 1.0.0
 */
@ApplicationScoped
public class PersonService implements RegisterPersonUseCase {

    private final PersonRepository personRepository;
    private final Counter          registeredCounter;
    private final Counter          suspendedCounter;
    private final Counter          reactivatedCounter;
    private final Counter          revokedCounter;
    private final Timer            registerTimer;

    /**
     * Constructor. Recibe dependencias por inyeccion de CDI.
     *
     * @param personRepository repositorio de personas
     * @param meterRegistry    registro de metricas de Micrometer
     * @throws NullPointerException si cualquier parametro es nulo
     */
    public PersonService(PersonRepository personRepository, MeterRegistry meterRegistry) {
        this.personRepository = Objects.requireNonNull(
                personRepository, "PersonRepository must not be null");

        Objects.requireNonNull(meterRegistry, "MeterRegistry must not be null");

        this.registeredCounter = Counter.builder("identity.persons.registered")
                .description("Total de personas registradas en el sistema")
                .tag("service", "identity-core")
                .register(meterRegistry);

        this.suspendedCounter = Counter.builder("identity.persons.suspended")
                .description("Total de personas suspendidas")
                .tag("service", "identity-core")
                .register(meterRegistry);

        this.reactivatedCounter = Counter.builder("identity.persons.reactivated")
                .description("Total de personas reactivadas")
                .tag("service", "identity-core")
                .register(meterRegistry);

        this.revokedCounter = Counter.builder("identity.persons.revoked")
                .description("Total de personas revocadas")
                .tag("service", "identity-core")
                .register(meterRegistry);

        this.registerTimer = Timer.builder("identity.persons.register.duration")
                .description("Duracion del proceso de registro de una persona")
                .tag("service", "identity-core")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws BusinessRuleException si ya existe una persona con la misma CURP
     */
    @Override
    @WithSpan("PersonService.register")
    public Person execute(@SpanAttribute("command.curp") Command command) {
        Objects.requireNonNull(command, "Command must not be null");

        return registerTimer.record(() -> {
            if (personRepository.existsByCurp(command.curp())) {
                throw new BusinessRuleException(
                        "PERSON_ALREADY_REGISTERED",
                        "A person with CURP " + command.curp() + " is already registered"
                );
            }

            Person person = Person.register(
                    command.curp(),
                    command.fullName(),
                    command.birthDate()
            );

            personRepository.save(person);
            registeredCounter.increment();

            return person;
        });
    }

    /**
     * Suspende a una persona identificada por su identificador unico.
     *
     * @param id     identificador unico de la persona
     * @param reason motivo de la suspension
     * @return aggregate Person con el estado actualizado
     * @throws NullPointerException      si id o reason son nulos
     * @throws ResourceNotFoundException si no existe una persona con ese id
     * @throws BusinessRuleException     si el estado actual no permite
     *                                   la transicion a SUSPENDED
     */
    @WithSpan("PersonService.suspend")
    public Person suspend(
            @SpanAttribute("person.id") PersonId id,
            @SpanAttribute("reason") String reason) {
        Objects.requireNonNull(id,     "PersonId must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");

        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PERSON_NOT_FOUND",
                        "Person with id " + id + " not found"
                ));

        person.suspend(reason);
        personRepository.update(person);
        suspendedCounter.increment();

        return person;
    }

    /**
     * Reactiva a una persona identificada por su identificador unico.
     *
     * @param id     identificador unico de la persona
     * @param reason motivo de la reactivacion
     * @return aggregate Person con el estado actualizado
     * @throws NullPointerException      si id o reason son nulos
     * @throws ResourceNotFoundException si no existe una persona con ese id
     * @throws BusinessRuleException     si el estado actual no permite
     *                                   la transicion a ACTIVE
     */
    @WithSpan("PersonService.reactivate")
    public Person reactivate(
            @SpanAttribute("person.id") PersonId id,
            @SpanAttribute("reason") String reason) {
        Objects.requireNonNull(id,     "PersonId must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");

        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PERSON_NOT_FOUND",
                        "Person with id " + id + " not found"
                ));

        person.reactivate(reason);
        personRepository.update(person);
        reactivatedCounter.increment();

        return person;
    }

    /**
     * Revoca a una persona identificada por su identificador unico.
     *
     * @param id     identificador unico de la persona
     * @param reason motivo de la revocacion
     * @return aggregate Person con el estado actualizado
     * @throws NullPointerException      si id o reason son nulos
     * @throws ResourceNotFoundException si no existe una persona con ese id
     * @throws BusinessRuleException     si el estado actual es terminal
     */
    @WithSpan("PersonService.revoke")
    public Person revoke(
            @SpanAttribute("person.id") PersonId id,
            @SpanAttribute("reason") String reason) {
        Objects.requireNonNull(id,     "PersonId must not be null");
        Objects.requireNonNull(reason, "Reason must not be null");

        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PERSON_NOT_FOUND",
                        "Person with id " + id + " not found"
                ));

        person.revoke(reason);
        personRepository.update(person);
        revokedCounter.increment();

        return person;
    }

    /**
     * Busca una persona por su identificador unico.
     *
     * @param id identificador unico de la persona
     * @return aggregate Person
     * @throws NullPointerException      si id es nulo
     * @throws ResourceNotFoundException si no existe una persona con ese id
     */
    @WithSpan("PersonService.findById")
    public Person findById(
            @SpanAttribute("person.id") PersonId id) {
        Objects.requireNonNull(id, "PersonId must not be null");

        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PERSON_NOT_FOUND",
                        "Person with id " + id + " not found"
                ));
    }

    /**
     * Busca una persona por su CURP.
     *
     * @param curp CURP del registro
     * @return aggregate Person
     * @throws NullPointerException      si curp es nula
     * @throws ResourceNotFoundException si no existe una persona con esa CURP
     */
    @WithSpan("PersonService.findByCurp")
    public Person findByCurp(
            @SpanAttribute("person.curp") Curp curp) {
        Objects.requireNonNull(curp, "Curp must not be null");

        return personRepository.findByCurp(curp)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PERSON_NOT_FOUND",
                        "Person with CURP " + curp + " not found"
                ));
    }
}