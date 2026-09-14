package com.pedidos360;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: levanta el contexto completo (auth + identity) contra H2.
 * Falla si algo del wiring esta mal: component scan, repos JPA, controllers,
 * security config.
 */
@SpringBootTest
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
