package com.finledger.core.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.finledger.core.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Customer Accounts through the public HTTP API, against a real Postgres.
 * Tests share one database, so each test creates its own Account and never
 * assumes the table is empty.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountApiTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void createdCustomerAccountStartsEmptyAndCanBeFetched() throws UnsupportedEncodingException {
        MvcTestResult created = createAccount("""
                {"ownerName": "Aisyah binti Ahmad"}
                """);

        assertThat(created).hasStatus(201);
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(created).hasHeader("Location", "/api/v1/accounts/" + id);
        assertThat(created).bodyJson().extractingPath("$.ownerName").isEqualTo("Aisyah binti Ahmad");
        assertThat(created).bodyJson().extractingPath("$.type").isEqualTo("CUSTOMER");
        assertThat(created).bodyJson().extractingPath("$.currency").isEqualTo("MYR");
        assertThat(created).bodyJson().extractingPath("$.balance").isEqualTo("0.00");
        assertThat(created).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
        assertThat(created).bodyJson().extractingPath("$.createdAt").isNotNull();

        MvcTestResult fetched = mvc.get().uri("/api/v1/accounts/{id}", id).exchange();

        assertThat(fetched).hasStatusOk();
        assertThat(fetched).bodyJson().extractingPath("$.id").isEqualTo(id);
        assertThat(fetched).bodyJson().extractingPath("$.ownerName").isEqualTo("Aisyah binti Ahmad");
        assertThat(fetched).bodyJson().extractingPath("$.balance").isEqualTo("0.00");
        assertThat(fetched).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
        // The create response must show what was stored, not a more precise in-memory value.
        String createdAt = JsonPath.read(created.getResponse().getContentAsString(), "$.createdAt");
        assertThat(fetched).bodyJson().extractingPath("$.createdAt").isEqualTo(createdAt);
    }

    @Test
    void clientCannotChooseBalanceTypeOrStatus() {
        MvcTestResult created = createAccount("""
                {"ownerName": "Wei Ling", "balance": "1000000.00", "type": "SYSTEM", "status": "FROZEN"}
                """);

        assertThat(created).hasStatus(201);
        assertThat(created).bodyJson().extractingPath("$.balance").isEqualTo("0.00");
        assertThat(created).bodyJson().extractingPath("$.type").isEqualTo("CUSTOMER");
        assertThat(created).bodyJson().extractingPath("$.status").isEqualTo("ACTIVE");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateRequests")
    void invalidOwnerNameIsRejectedWithBadRequest(String description, String json) {
        MvcTestResult result = createAccount(json);

        assertThat(result).hasStatus(400);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(400);
    }

    static Stream<Arguments> invalidCreateRequests() {
        return Stream.of(
                Arguments.of("blank ownerName", """
                        {"ownerName": "   "}"""),
                Arguments.of("empty ownerName", """
                        {"ownerName": ""}"""),
                Arguments.of("null ownerName", """
                        {"ownerName": null}"""),
                Arguments.of("missing ownerName", "{}"),
                Arguments.of("ownerName longer than 100 characters",
                        "{\"ownerName\": \"" + "a".repeat(101) + "\"}"));
    }

    @Test
    void unknownAccountIdIsNotFound() {
        MvcTestResult result = mvc.get()
                .uri("/api/v1/accounts/{id}", "00000000-0000-0000-0000-000000000000")
                .exchange();

        assertThat(result).hasStatus(404);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(404);
    }

    @Test
    void malformedAccountIdIsBadRequest() {
        MvcTestResult result = mvc.get().uri("/api/v1/accounts/{id}", "not-a-uuid").exchange();

        assertThat(result).hasStatus(400);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private MvcTestResult createAccount(String json) {
        return mvc.post().uri("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }
}
