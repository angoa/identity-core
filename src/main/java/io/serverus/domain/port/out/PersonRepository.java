package io.serverus.domain.port.out;

import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.PersonId;

import java.util.Optional;

/**
 * Port de salida que define el contrato de persistencia del aggregate Person.
 *
 * La implementacion de este puerto vive en la capa de infraestructura
 * — nunca en el dominio. El dominio solo conoce esta interfaz.
 *
 * El adaptador de persistencia que implemente este puerto es responsable
 * de la traduccion entre el aggregate Person y la representacion
 * en base de datos.
 *
 * @since 1.0.0
 */
public interface PersonRepository {

    /**
     * Persiste un nuevo aggregate Person.
     *
     * @param person aggregate a persistir
     * @throws io.serverus.domain.exception.BusinessRuleException si ya existe
     *         una persona con la misma CURP
     */
    void save(Person person);

    /**
     * Actualiza el estado de un aggregate Person existente.
     *
     * @param person aggregate con el estado actualizado
     */
    void update(Person person);

    /**
     * Busca un aggregate Person por su identificador unico.
     *
     * @param id identificador unico del aggregate
     * @return Optional con el aggregate si existe, empty si no existe
     */
    Optional<Person> findById(PersonId id);

    /**
     * Busca un aggregate Person por su CURP.
     *
     * @param curp CURP del registro
     * @return Optional con el aggregate si existe, empty si no existe
     */
    Optional<Person> findByCurp(Curp curp);

    /**
     * Verifica si existe una persona registrada con la CURP indicada.
     *
     * Se utiliza para validar duplicados antes de registrar una nueva persona.
     *
     * @param curp CURP a verificar
     * @return true si ya existe una persona con esa CURP
     */
    boolean existsByCurp(Curp curp);
}