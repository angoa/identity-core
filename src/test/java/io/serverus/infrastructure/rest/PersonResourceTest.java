package io.serverus.infrastructure.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests de integracion del adaptador REST PersonResource.
 *
 * Levanta el contenedor Quarkus completo con H2 en memoria.
 * Quarkus reutiliza la misma instancia JVM para PersonRepositoryImplTest
 * y PersonResourceTest, por lo tanto la H2 es compartida.
 *
 * Estrategia de aislamiento: CURPs unicas por test, con prefijo "RC"
 * (Resource Test) para evitar colisiones con las CURPs del
 * PersonRepositoryImplTest (prefijo "RE", "LO", "GU", etc.).
 *
 * Los PATCH envian {@code {"reason":"..."}} como JSON —
 * PersonResource declara @Consumes(APPLICATION_JSON) y recibe
 * un StatusRequest deserializado por Jackson.
 *
 * @since 1.0.0
 */
@QuarkusTest
@DisplayName("PersonResource — integracion REST")
class PersonResourceTest {

    // -------------------------------------------------------------------------
    // Payloads de registro — prefijo "RC" garantiza no colision con PersonRepositoryImplTest
    // -------------------------------------------------------------------------

    private static final String PAYLOAD_POST_201 = """
            {
                "curp": "RCAA800101HDFRRN01",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1980-01-01"
            }""";

    private static final String PAYLOAD_GET_ID = """
            {
                "curp": "RCBB810215HDFRRN02",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1981-02-15"
            }""";

    private static final String PAYLOAD_SUSPEND_200 = """
            {
                "curp": "RCCC820320HDFRRN03",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1982-03-20"
            }""";

    private static final String PAYLOAD_SUSPEND_409 = """
            {
                "curp": "RCDD830425HDFRRN04",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1983-04-25"
            }""";

    private static final String PAYLOAD_REACTIVATE_200 = """
            {
                "curp": "RCEE840530HDFRRN05",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1984-05-30"
            }""";

    private static final String PAYLOAD_REACTIVATE_409 = """
            {
                "curp": "RCFF850615HDFRRN06",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1985-06-15"
            }""";

    private static final String PAYLOAD_REVOKE_200 = """
            {
                "curp": "RCGG860720HDFRRN07",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1986-07-20"
            }""";

    private static final String PAYLOAD_REVOKE_409 = """
            {
                "curp": "RCHH870825HDFRRN08",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1987-08-25"
            }""";

    private static final String PAYLOAD_GET_CURP = """
            {
                "curp": "RCII880930HDFRRN09",
                "givenName": "Rafael",
                "paternalSurname": "Morales",
                "maternalSurname": "Angoa",
                "birthDate": "1988-09-30"
            }""";

    private static final String REASON_JSON = """
            {"reason": "Motivo de prueba"}""";

    // -------------------------------------------------------------------------
    // Helper — registra y devuelve el ID
    // -------------------------------------------------------------------------

    private String registerAndGetId(String payload) {
        return given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when().post("/persons")
                .then().statusCode(201)
                .extract().path("id");
    }

    // -------------------------------------------------------------------------
    // POST /persons
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /persons")
    class Register {

        @Test
        @DisplayName("retorna 201 con la persona registrada cuando los datos son validos")
        void shouldReturn201WhenPersonIsValid() {
            given()
                    .contentType(ContentType.JSON)
                    .body(PAYLOAD_POST_201)
                    .when().post("/persons")
                    .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("curp", equalTo("RCAA800101HDFRRN01"))
                    .body("status", equalTo("ACTIVE"));
        }

        @Test
        @DisplayName("retorna 409 cuando la CURP ya esta registrada")
        void shouldReturn409WhenCurpAlreadyRegistered() {
            // Primera vez: se registra en shouldReturn201WhenPersonIsValid o aqui
            given()
                    .contentType(ContentType.JSON)
                    .body(PAYLOAD_POST_201)
                    .when().post("/persons");

            // Segunda vez: debe retornar 409
            given()
                    .contentType(ContentType.JSON)
                    .body(PAYLOAD_POST_201)
                    .when().post("/persons")
                    .then()
                    .statusCode(409)
                    .body("errorCode", equalTo("PERSON_ALREADY_REGISTERED"));
        }

        @Test
        @DisplayName("retorna 400 cuando la CURP tiene formato invalido")
        void shouldReturn422WhenCurpIsInvalid() {
            String invalidPayload = """
                    {
                        "curp": "INVALIDA",
                        "givenName": "Rafael",
                        "paternalSurname": "Morales",
                        "maternalSurname": "Angoa",
                        "birthDate": "1980-01-01"
                    }""";

            given()
                    .contentType(ContentType.JSON)
                    .body(invalidPayload)
                    .when().post("/persons")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("retorna 400 cuando falta el nombre")
        void shouldReturn422WhenNameIsMissing() {
            String missingName = """
                    {
                        "curp": "RCAA800101HDFRRN01",
                        "paternalSurname": "Morales",
                        "birthDate": "1980-01-01"
                    }""";

            given()
                    .contentType(ContentType.JSON)
                    .body(missingName)
                    .when().post("/persons")
                    .then()
                    .statusCode(400);
        }
    }

    // -------------------------------------------------------------------------
    // GET /persons/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /persons/{id}")
    class FindById {

        @Test
        @DisplayName("retorna 200 con la persona cuando el ID existe")
        void shouldReturn200WhenPersonExists() {
            String id = registerAndGetId(PAYLOAD_GET_ID);

            given()
                    .when().get("/persons/" + id)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("curp", equalTo("RCBB810215HDFRRN02"));
        }

        @Test
        @DisplayName("retorna 404 cuando el ID no existe")
        void shouldReturn404WhenPersonNotFound() {
            given()
                    .when().get("/persons/00000000-0000-0000-0000-000000000000")
                    .then()
                    .statusCode(404)
                    .body("errorCode", equalTo("PERSON_NOT_FOUND"));
        }

        @Test
        @DisplayName("retorna 400 cuando el ID tiene formato invalido")
        void shouldReturn400WhenIdIsInvalid() {
            given()
                    .when().get("/persons/not-a-uuid")
                    .then()
                    .statusCode(400);
        }
    }

    // -------------------------------------------------------------------------
    // GET /persons/curp/{curp}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /persons/curp/{curp}")
    class FindByCurp {

        @Test
        @DisplayName("retorna 200 con la persona cuando la CURP existe")
        void shouldReturn200WhenCurpExists() {
            registerAndGetId(PAYLOAD_GET_CURP);

            given()
                    .when().get("/persons/curp/RCII880930HDFRRN09")
                    .then()
                    .statusCode(200)
                    .body("curp", equalTo("RCII880930HDFRRN09"));
        }

        @Test
        @DisplayName("retorna 404 cuando la CURP no existe en la base de datos")
        void shouldReturn404WhenCurpNotFound() {
            // CURP valida formato RENAPO, inexistente en BD
            given()
                    .when().get("/persons/curp/XEXX010101MNEXXXA8")
                    .then()
                    .statusCode(404)
                    .body("errorCode", equalTo("PERSON_NOT_FOUND"));
        }

        @Test
        @DisplayName("retorna 400 cuando la CURP tiene formato invalido")
        void shouldReturn400WhenCurpIsInvalid() {
            given()
                    .when().get("/persons/curp/INVALIDA")
                    .then()
                    .statusCode(400);
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /persons/{id}/suspend
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /persons/{id}/suspend")
    class Suspend {

        @Test
        @DisplayName("retorna 200 con estado SUSPENDED cuando la persona esta ACTIVE")
        void shouldReturn200WithSuspendedStatus() {
            String id = registerAndGetId(PAYLOAD_SUSPEND_200);

            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/suspend")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("SUSPENDED"));
        }

        @Test
        @DisplayName("retorna 404 cuando el ID no existe")
        void shouldReturn404WhenPersonNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/00000000-0000-0000-0000-000000000000/suspend")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("retorna 409 cuando la persona ya esta SUSPENDED")
        void shouldReturn409WhenAlreadySuspended() {
            String id = registerAndGetId(PAYLOAD_SUSPEND_409);

            // Primera suspension — debe ser 200
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/suspend")
                    .then().statusCode(200);

            // Segunda suspension — debe ser 409
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/suspend")
                    .then()
                    .statusCode(409)
                    .body("errorCode", notNullValue());
        }

        @Test
        @DisplayName("retorna 400 cuando el ID tiene formato invalido")
        void shouldReturn400WhenIdIsInvalid() {
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/not-a-uuid/suspend")
                    .then()
                    .statusCode(400);
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /persons/{id}/reactivate
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /persons/{id}/reactivate")
    class Reactivate {

        @Test
        @DisplayName("retorna 200 con estado ACTIVE cuando la persona esta SUSPENDED")
        void shouldReturn200WithActiveStatus() {
            String id = registerAndGetId(PAYLOAD_REACTIVATE_200);

            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/suspend")
                    .then().statusCode(200);

            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/reactivate")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("ACTIVE"));
        }

        @Test
        @DisplayName("retorna 404 cuando el ID no existe")
        void shouldReturn404WhenPersonNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/00000000-0000-0000-0000-000000000000/reactivate")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("retorna 409 cuando la persona ya esta ACTIVE")
        void shouldReturn409WhenPersonIsAlreadyActive() {
            String id = registerAndGetId(PAYLOAD_REACTIVATE_409);

            // Persona recien registrada esta ACTIVE — reactivar debe ser 409
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/reactivate")
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("retorna 400 cuando el ID tiene formato invalido")
        void shouldReturn400WhenIdIsInvalid() {
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/not-a-uuid/reactivate")
                    .then()
                    .statusCode(400);
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /persons/{id}/revoke
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /persons/{id}/revoke")
    class Revoke {

        @Test
        @DisplayName("retorna 200 con estado REVOKED")
        void shouldReturn200WithRevokedStatus() {
            String id = registerAndGetId(PAYLOAD_REVOKE_200);

            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/revoke")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("REVOKED"));
        }

        @Test
        @DisplayName("retorna 404 cuando el ID no existe")
        void shouldReturn404WhenPersonNotFound() {
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/00000000-0000-0000-0000-000000000000/revoke")
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("retorna 409 cuando la persona ya esta REVOKED")
        void shouldReturn409WhenAlreadyRevoked() {
            String id = registerAndGetId(PAYLOAD_REVOKE_409);

            // Primera revocacion — debe ser 200
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/revoke")
                    .then().statusCode(200);

            // Segunda revocacion — debe ser 409
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/" + id + "/revoke")
                    .then()
                    .statusCode(409);
        }

        @Test
        @DisplayName("retorna 400 cuando el ID tiene formato invalido")
        void shouldReturn400WhenIdIsInvalid() {
            given()
                    .contentType(ContentType.JSON)
                    .body(REASON_JSON)
                    .when().patch("/persons/not-a-uuid/revoke")
                    .then()
                    .statusCode(400);
        }
    }
}