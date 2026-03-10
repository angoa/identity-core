package io.serverus.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de salida para la representacion estandar de errores en el API.
 *
 * Todos los errores del API retornan esta estructura, permitiendo
 * al cliente identificar el error de forma programatica mediante
 * el errorCode y construir mensajes de usuario apropiados.
 *
 * @param errorCode  codigo de error unico del dominio
 * @param message    mensaje descriptivo del error en ingles
 * @param field      campo que origino el error, puede ser nulo
 * @param timestamp  fecha y hora del error
 * @param path       path del endpoint que origino el error
 *
 * @since 1.0.0
 */
@Schema(name = "ErrorResponse", description = "Estructura estandar de errores del API")
public record ErrorResponse(
        @Schema(description = "Codigo de error unico del dominio")
        String errorCode,
        @Schema(description = "Mensaje descriptivo del error")
        String message,
        @Schema(description = "Campo que origino el error, nulo si no aplica")
        String field,
        @Schema(description = "Fecha y hora del error")
        LocalDateTime timestamp,
        @Schema(description = "Path del endpoint")
        String path
) {
    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(errorCode, message, null, LocalDateTime.now(), path);
    }

    public static ErrorResponse ofField(
            String errorCode, String message, String field, String path) {
        return new ErrorResponse(errorCode, message, field, LocalDateTime.now(), path);
    }
}