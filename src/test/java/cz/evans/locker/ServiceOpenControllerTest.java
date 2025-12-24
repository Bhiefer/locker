package cz.evans.locker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceOpenControllerTest {

    static Path dbFile;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        dbFile = Files.createTempFile("locker-service-", ".db");
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbFile.toAbsolutePath());
        r.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        r.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @BeforeEach
    void seed() {
        // Minimální seed: jen lokace
        jdbc.update("DELETE FROM locations");
        jdbc.update("""
      INSERT INTO locations(id, code, name, service_pin_code, service_pin_failed_attempts, service_pin_max_attempts, service_pin_locked_until)
      VALUES (1, 'FM1', 'Frýdek-Místek 1', '834927', 0, 3, NULL)
    """);
    }

    @Test
    void service_open_ok() throws Exception {
        mvc.perform(post("/api/v1/service/open")
                        .contentType(APPLICATION_JSON)
                        .content("""
          {"locationCode":"FM1","servicePin":"834927","boxNo":4}
        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.boxNo").value(4));
    }

    @Test
    void service_open_wrong_pin_denied() throws Exception {
        mvc.perform(post("/api/v1/service/open")
                        .contentType(APPLICATION_JSON)
                        .content("""
          {"locationCode":"FM1","servicePin":"000000","boxNo":4}
        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("DENY"))
                .andExpect(jsonPath("$.reason").value("INVALID_SERVICE_PIN"));

        Integer attempts = jdbc.queryForObject("""
      SELECT service_pin_failed_attempts FROM locations WHERE code='FM1'
    """, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, attempts);
    }

    @Test
    void service_open_locks_after_max_attempts() throws Exception {
        // max_attempts = 3 (ze seed)
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/service/open")
                            .contentType(APPLICATION_JSON)
                            .content("""
            {"locationCode":"FM1","servicePin":"000000","boxNo":1}
          """))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/v1/service/open")
                        .contentType(APPLICATION_JSON)
                        .content("""
          {"locationCode":"FM1","servicePin":"834927","boxNo":1}
        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("DENY"))
                .andExpect(jsonPath("$.reason").value("LOCKED"));
    }
}
