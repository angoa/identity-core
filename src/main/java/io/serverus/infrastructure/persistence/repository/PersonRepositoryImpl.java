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

import java.util.Optional;

/**
 * Adaptador de persistencia que implementa el port PersonRepository
 * utilizando JPA con Oracle como base de datos.
 *
 * Traduce entre el aggregate Person del dominio y la entidad JPA
 * PersonEntity utilizando PersonMapper. El dominio nunca conoce
 * esta implementacion — solo conoce la interfaz PersonRepository.
 *
 * @since 1.0.0
 */
@ApplicationScoped
public class PersonRepositoryImpl implements PersonRepository {

    @PersistenceContext
    EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void save(Person person) {
        PersonEntity entity = PersonMapper.toEntity(person);
        entityManager.persist(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void update(Person person) {
        PersonEntity entity = PersonMapper.toEntity(person);
        entityManager.merge(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Person> findById(PersonId id) {
        PersonEntity entity = entityManager.find(PersonEntity.class, id.toString());
        return Optional.ofNullable(entity).map(PersonMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     */
    @Override
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