package io.serverus.infrastructure.rest.dto;

import io.serverus.domain.model.vo.BirthDate;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.FullName;
import io.serverus.domain.port.in.RegisterPersonUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO de entrada para el registro de una persona.
 *
 * Define el contrato de la API para el endpoint POST /persons.
 * Valida el formato de los campos antes de construir los Value Objects
 * del dominio, proporcionando mensajes de error precisos al cliente.
 *
 * @param curp            CURP del registro en formato RENAPO
 * @param givenName       nombre de pila
 * @param paternalSurname apellido paterno
 * @param maternalSurname apellido materno, opcional
 * @param birthDate       fecha de nacimiento en formato ISO-8601
 *
 * @since 1.0.0
 */
@Schema(name = "PersonRequest", description = "Datos para el registro de una persona")
public record PersonRequest(

        @NotBlank(message = "curp is required")
        @Pattern(
                regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9][0-9]$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "curp does not match RENAPO official format"
        )
        @Schema(description = "CURP en formato oficial RENAPO")
        String curp,

        @NotBlank(message = "givenName is required")
        @Schema(description = "Nombre de pila")
        String givenName,

        @NotBlank(message = "paternalSurname is required")
        @Schema(description = "Apellido paterno")
        String paternalSurname,

        @Schema(description = "Apellido materno, opcional")
        String maternalSurname,

        @NotBlank(message = "birthDate is required")
        @Pattern(
                regexp = "^\\d{4}-\\d{2}-\\d{2}$",
                message = "birthDate must be in ISO-8601 format (YYYY-MM-DD)"
        )
        @Schema(description = "Fecha de nacimiento en formato ISO-8601")
        String birthDate

) {
    /**
     * Convierte el DTO a un comando del caso de uso.
     *
     * @return comando listo para ejecutar el caso de uso
     */
    public RegisterPersonUseCase.Command toCommand() {
        return new RegisterPersonUseCase.Command(
                Curp.of(curp),
                FullName.of(givenName, paternalSurname, maternalSurname),
                BirthDate.of(birthDate)
        );
    }
}