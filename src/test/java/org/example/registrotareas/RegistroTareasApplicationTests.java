package org.example.registrotareas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // Propiedades directas (sin pasar por ${VAR_ENTORNO})
        "server.port=8081",
        "app.jwt.secret=test-secret-key-for-unit-tests-only-minimum32chars",
        "app.jwt.expiration-ms=86400000",
        "google.oauth.client-id=test-client-id",
        "google.oauth.client-secret=test-client-secret",
        "google.oauth.refresh-token=test-refresh-token",
        "google.drive.root-folder=TestFolder",
        // H2 en memoria — sin necesitar PostgreSQL instalado
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RegistroTareasApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores de configuración
    }
}
