package io.serverus.domain.port.in;

import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;

/**
 * Port de entrada que define el caso de uso de registro de una persona
 * en el sistema de identidad digital.
 *
 * La implementacion de este puerto vive en la capa de dominio —
 * en el servicio de dominio PersonService.
 *
 * @since 1.0.0
 */
public interface RegisterPersonUseCase {

    /**
     * Registra una nueva persona en el sistema de identidad digital.
     *
     * Verifica que no exista una persona registrada con la misma CURP,
     * crea el aggregate Person y lo persiste a traves del repositorio.
     *
     * @param command comando con los datos del registro
     * @return aggregate Person recien registrado
     * @throws io.serverus.domain.exception.BusinessRuleException si ya existe
     *         una persona registrada con la misma CURP
     */
    Person execute(Command command);

    /**
     * Comando que encapsula los datos necesarios para registrar una persona.
     *
     * Sigue el patron Command para desacoplar el caso de uso de los
     * detalles de transporte — el REST adapter construye el comando
     * desde el DTO de entrada sin conocer la implementacion del caso de uso.
     *
     * @param curp      CURP del registro
     * @param fullName  nombre completo
     * @param birthDate fecha de nacimiento
     */
    record Command(Curp curp, FullName fullName, BirthDate birthDate) {

        public Command {
            java.util.Objects.requireNonNull(curp,      "Command curp must not be null");
            java.util.Objects.requireNonNull(fullName,  "Command fullName must not be null");
            java.util.Objects.requireNonNull(birthDate, "Command birthDate must not be null");
        }
    }
}