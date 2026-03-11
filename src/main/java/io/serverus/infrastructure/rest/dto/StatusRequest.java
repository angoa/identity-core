package io.serverus.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para las operaciones de cambio de estado de una persona.
 *
 * Encapsula el motivo requerido por las operaciones suspend, reactivate y revoke.
 *
 * @since 1.0.0
 */
public final class StatusRequest {

    @NotBlank(message = "El motivo no puede estar vacio")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
    private final String reason;

    @JsonCreator
    public StatusRequest(@JsonProperty("reason") String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}