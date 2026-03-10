package io.serverus.infrastructure.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Configuracion central de OpenAPI para el sistema de identidad digital.
 *
 * Define los metadatos del API, los servidores disponibles, las etiquetas
 * de agrupacion de endpoints y el esquema de seguridad JWT utilizado
 * por Keycloak y Llave MX.
 *
 * @since 1.0.0
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Identity Core API",
                version = "1.0.0",
                description = "Servicio de gestion de identidad digital. " +
                        "Administra el ciclo de vida de personas, atributos y credenciales. " +
                        "Referencia de implementacion para la migracion del stack ATDT.",
                contact = @Contact(
                        name = "Arquitectura ATDT",
                        email = "arquitectura@atdt.gob.mx"
                ),
                license = @License(
                        name = "Uso interno ATDT",
                        url = "https://atdt.gob.mx"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Desarrollo local"),
                @Server(url = "https://identity.atdt.gob.mx", description = "Produccion")
        },
        tags = {
                @Tag(name = "Persons", description = "Registro y gestion de personas"),
                @Tag(name = "Attributes", description = "Atributos verificados de identidad"),
                @Tag(name = "Credentials", description = "Ciclo de vida de credenciales digitales")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT emitido por Keycloak o Llave MX"
)
public class OpenApiConfig extends Application {
}