package io.serverus.infrastructure.rest;

import io.serverus.domain.exception.BusinessRuleException;
import io.serverus.domain.exception.ResourceNotFoundException;
import io.serverus.domain.exception.ValidationException;
import io.serverus.domain.model.Person;
import io.serverus.domain.model.vo.Curp;
import io.serverus.domain.model.vo.PersonId;
import io.serverus.domain.service.PersonService;
import io.serverus.infrastructure.rest.dto.ErrorResponse;
import io.serverus.infrastructure.rest.dto.PersonRequest;
import io.serverus.infrastructure.rest.dto.PersonResponse;
import io.serverus.infrastructure.rest.dto.StatusRequest;
import io.serverus.infrastructure.rest.dto.StatusResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Adaptador REST que expone los casos de uso de gestion de personas.
 *
 * Traduce las peticiones HTTP a comandos del dominio y las respuestas
 * del dominio a DTOs de salida. Maneja las excepciones del dominio
 * y las mapea a codigos HTTP apropiados.
 *
 * No contiene logica de negocio — delega al PersonService.
 *
 * @since 1.0.0
 */
@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Persons")
public class PersonResource {

    @Inject
    PersonService personService;

    @Context
    UriInfo uriInfo;

    @POST
    @Operation(summary = "Registrar persona", description = "Registra una nueva persona en el sistema de identidad digital")
    @RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = PersonRequest.class))
    )
    @APIResponse(responseCode = "201", description = "Persona registrada exitosamente",
            content = @Content(schema = @Schema(implementation = PersonResponse.class)))
    @APIResponse(responseCode = "409", description = "CURP ya registrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Datos invalidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response register(@Valid PersonRequest request) {
        try {
            Person person = personService.execute(request.toCommand());
            return Response.status(Response.Status.CREATED)
                    .entity(PersonResponse.from(person))
                    .build();
        } catch (BusinessRuleException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (ValidationException e) {
            return Response.status(422)
                    .entity(ErrorResponse.ofField(e.getErrorCode(), e.getMessage(), e.getField(), uriInfo.getPath()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar persona por ID")
    @APIResponse(responseCode = "200", description = "Persona encontrada",
            content = @Content(schema = @Schema(implementation = PersonResponse.class)))
    @APIResponse(responseCode = "400", description = "ID invalido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Persona no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response findById(@PathParam("id") String id) {
        try {
            PersonId personId = PersonId.of(id);
            Person person = personService.findById(personId);
            return Response.ok(PersonResponse.from(person)).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (ResourceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        }
    }

    @GET
    @Path("/curp/{curp}")
    @Operation(summary = "Buscar persona por CURP")
    @APIResponse(responseCode = "200", description = "Persona encontrada",
            content = @Content(schema = @Schema(implementation = PersonResponse.class)))
    @APIResponse(responseCode = "400", description = "CURP invalida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Persona no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response findByCurp(@PathParam("curp") String curp) {
        try {
            Person person = personService.findByCurp(Curp.of(curp));
            return Response.ok(PersonResponse.from(person)).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (ResourceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        }
    }

    @PATCH
    @Path("/{id}/suspend")
    @Operation(summary = "Suspender persona")
    @RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = StatusRequest.class))
    )
    @APIResponse(responseCode = "200", description = "Persona suspendida exitosamente",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @APIResponse(responseCode = "404", description = "Persona no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Transicion de estado no permitida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response suspend(@PathParam("id") String id, @Valid StatusRequest request) {
        try {
            PersonId personId = PersonId.of(id);
            Person person = personService.suspend(personId, request.getReason());
            return Response.ok(StatusResponse.from(person)).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (ResourceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (BusinessRuleException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        }
    }

    @PATCH
    @Path("/{id}/reactivate")
    @Operation(summary = "Reactivar persona")
    @RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = StatusRequest.class))
    )
    @APIResponse(responseCode = "200", description = "Persona reactivada exitosamente",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @APIResponse(responseCode = "404", description = "Persona no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Transicion de estado no permitida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response reactivate(@PathParam("id") String id, @Valid StatusRequest request) {
        try {
            PersonId personId = PersonId.of(id);
            Person person = personService.reactivate(personId, request.getReason());
            return Response.ok(StatusResponse.from(person)).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (ResourceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (BusinessRuleException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        }
    }

    @PATCH
    @Path("/{id}/revoke")
    @Operation(summary = "Revocar persona")
    @RequestBody(
            required = true,
            content = @Content(schema = @Schema(implementation = StatusRequest.class))
    )
    @APIResponse(responseCode = "200", description = "Persona revocada exitosamente",
            content = @Content(schema = @Schema(implementation = StatusResponse.class)))
    @APIResponse(responseCode = "404", description = "Persona no encontrada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Transicion de estado no permitida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response revoke(@PathParam("id") String id, @Valid StatusRequest request) {
        try {
            PersonId personId = PersonId.of(id);
            Person person = personService.revoke(personId, request.getReason());
            return Response.ok(StatusResponse.from(person)).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (ResourceNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        } catch (BusinessRuleException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ErrorResponse.of(e.getErrorCode(), e.getMessage(), uriInfo.getPath()))
                    .build();
        }
    }
}