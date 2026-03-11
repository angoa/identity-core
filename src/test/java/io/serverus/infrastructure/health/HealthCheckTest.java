package io.serverus.infrastructure.health;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests de integracion del endpoint de health de Quarkus.
 *
 * Verifica que los tres endpoints estandar de SmallRye Health
 * respondan correctamente, y que el readiness check personalizado
 * {@code PersonRepositoryHealthCheck} reporte UP cuando H2 esta
 * disponible en el perfil de test.
 *
 * Los endpoints de health no requieren autenticacion — se acceden
 * en /q/health/* por convencion de Quarkus.
 *
 * @since 1.0.0
 */
@QuarkusTest
@DisplayName("Health endpoints")
class HealthCheckTest {

    @Test
    @DisplayName("/q/health/live retorna 200 UP")
    void shouldReturnUpOnLiveness() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("/q/health/ready retorna 200 UP con PersonRepositoryHealthCheck activo")
    void shouldReturnUpOnReadiness() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("/q/health retorna 200 UP (liveness + readiness combinados)")
    void shouldReturnUpOnCombinedHealth() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}