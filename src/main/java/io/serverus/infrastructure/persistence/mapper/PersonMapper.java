package io.serverus.infrastructure.persistence.mapper;

import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.infrastructure.persistence.entity.PersonEntity;

/**
 * Mapper que traduce entre el aggregate Person del dominio y la
 * entidad JPA PersonEntity de la capa de persistencia.
 *
 * Centraliza la logica de traduccion en un unico lugar, manteniendo
 * el aggregate limpio de dependencias de infraestructura y la entidad
 * JPA limpia de logica de dominio.
 *
 * @since 1.0.0
 */
public class PersonMapper {

    private PersonMapper() {
        // Clase utilitaria — no instanciable
    }

    /**
     * Traduce un aggregate Person del dominio a una entidad JPA PersonEntity.
     *
     * Se utiliza en el persistence adapter antes de persistir o actualizar
     * el aggregate en la base de datos.
     *
     * @param person aggregate a traducir
     * @return entidad JPA lista para persistir
     */
    public static PersonEntity toEntity(Person person) {
        return PersonEntity.builder()
                .id(person.getId().toString())
                .curp(person.getCurp().toString())
                .givenName(person.getFullName().givenName())
                .paternalSurname(person.getFullName().paternalSurname())
                .maternalSurname(person.getFullName().maternalSurname())
                .birthDate(person.getBirthDate().value())
                .status(person.getStatus())
                .createdAt(person.getCreatedAt())
                .updatedAt(person.getUpdatedAt())
                .build();
    }

    /**
     * Traduce una entidad JPA PersonEntity a un aggregate Person del dominio.
     *
     * Se utiliza en el persistence adapter al reconstruir el aggregate
     * desde la base de datos. Utiliza Person.reconstitute() para garantizar
     * que el aggregate se construye correctamente sin disparar logica
     * de negocio de creacion.
     *
     * @param entity entidad JPA a traducir
     * @return aggregate Person reconstruido
     */
    public static Person toDomain(PersonEntity entity) {
        return Person.reconstitute(
                PersonId.of(entity.getId()),
                Curp.of(entity.getCurp()),
                FullName.of(
                        entity.getGivenName(),
                        entity.getPaternalSurname(),
                        entity.getMaternalSurname()
                ),
                BirthDate.of(entity.getBirthDate()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}