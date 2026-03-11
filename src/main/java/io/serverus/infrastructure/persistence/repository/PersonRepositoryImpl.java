package io.serverus.infrastructure.persistence.repository;

import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.port.out.PersonRepository;
import io.serverus.infrastructure.persistence.entity.PersonEntity;
import io.serverus.infrastructure.persistence.mapper.PersonMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.util.Optional;

/**
 * Adaptador de persistencia que implementa el port PersonRepository
 * utilizando JPA con Oracle como base de datos de produccion.
 *
 * <h3>Resiliencia</h3>
 * Cada metodo esta protegido con fault tolerance de MicroProfile:
 * <ul>
 *   <li>{@code @Timeout} — aborta la operacion si Oracle no responde
 *       dentro del umbral configurado, evitando que threads bloqueados
 *       saturen el pool de conexiones.</li>
 *   <li>{@code @CircuitBreaker} — abre el circuito si la tasa de fallos
 *       supera el umbral, devolviendo fallo rapido en lugar de acumular
 *       threads bloqueados mientras la BD esta degradada.</li>
 *   <li>{@code @Retry} — reintenta operaciones de lectura ante fallos
 *       transitorios de red. No se aplica a escrituras para evitar
 *       inserciones duplicadas.</li>
 * </ul>
 *
 * Los umbrales de cada politica son configurables via propiedades
 * {@code mp.fault-tolerance.*} — ver {@code application.yaml}.
 *
 * @since 1.0.0
 */
@ApplicationScoped
public class PersonRepositoryImpl implements PersonRepository {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * {@inheritDoc}
     *
     * <p>Timeout de 5s — las escrituras con commit en Oracle pueden tener
     * latencia legitima mayor que las lecturas, pero un timeout de 5s es
     * suficiente para detectar una BD completamente bloqueada.
     *
     * <p>No se aplica {@code @Retry} — reintentar un persist duplicaria
     * el registro si el primer intento persistio pero el ACK se perdio.
     */
    @Override
    @Transactional
    @Timeout(5000)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio           = 0.5,
            delay                  = 30000,
            successThreshold       = 2
    )
    public void save(Person person) {
        PersonEntity entity = PersonMapper.toEntity(person);
        entityManager.persist(entity);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Misma politica que {@code save} — no se reintenta para evitar
     * actualizaciones repetidas con efectos secundarios.
     */
    @Override
    @Transactional
    @Timeout(5000)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio           = 0.5,
            delay                  = 30000,
            successThreshold       = 2
    )
    public void update(Person person) {
        PersonEntity entity = PersonMapper.toEntity(person);
        entityManager.merge(entity);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Timeout de 2s — las lecturas por PK son inmediatas en condiciones
     * normales; superar 2s indica una BD degradada.
     *
     * <p>{@code @Retry(maxRetries=2)} — reintenta ante fallos transitorios
     * de red o timeouts de conexion del pool antes de abrir el circuito.
     */
    @Override
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio           = 0.5,
            delay                  = 30000,
            successThreshold       = 2
    )
    public Optional<Person> findById(PersonId id) {
        PersonEntity entity = entityManager.find(PersonEntity.class, id.toString());
        return Optional.ofNullable(entity).map(PersonMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Timeout de 2s — query por columna indexada (CURP tiene indice
     * unico en produccion). Tiempo superior indica degradacion.
     */
    @Override
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio           = 0.5,
            delay                  = 30000,
            successThreshold       = 2
    )
    public Optional<Person> findByCurp(Curp curp) {
        try {
            PersonEntity entity = entityManager
                    .createQuery(
                            "SELECT p FROM PersonEntity p WHERE p.curp = :curp",
                            PersonEntity.class
                    )
                    .setParameter("curp", curp.toString())
                    .getSingleResult();
            return Optional.of(PersonMapper.toDomain(entity));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Timeout de 2s — COUNT sobre columna indexada es O(1) en Oracle
     * con indice unico. Se usa antes de cada {@code save} para verificar
     * duplicados — si esta operacion falla rapido, el registro falla rapido.
     */
    @Override
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio           = 0.5,
            delay                  = 30000,
            successThreshold       = 2
    )
    public boolean existsByCurp(Curp curp) {
        Long count = entityManager
                .createQuery(
                        "SELECT COUNT(p) FROM PersonEntity p WHERE p.curp = :curp",
                        Long.class
                )
                .setParameter("curp", curp.toString())
                .getSingleResult();
        return count > 0;
    }
}