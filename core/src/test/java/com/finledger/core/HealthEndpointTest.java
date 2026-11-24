package com.finledger.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Proves the app is wired to a real Postgres: the health endpoint must report
 * the database component itself as UP, not just the app as a whole.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthEndpointTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void healthIsUpAndIncludesTheDatabase() {
        var response = mvc.get().uri("/actuator/health").exchange();

        assertThat(response).hasStatusOk();
        assertThat(response).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(response).bodyJson().extractingPath("$.components.db.status").isEqualTo("UP");
    }
}
