package cz.evans.locker;

import cz.evans.locker.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenControllerTest {

    static Path dbFile;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        dbFile = Files.createTempFile("locker-web-", ".db");
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbFile.toAbsolutePath());
        r.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        r.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationService reservationService;
    @Autowired MockMvc mvc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM boxes");
        jdbc.update("DELETE FROM product_variations");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM locations");

        jdbc.update("INSERT INTO locations(id, code, name) VALUES (1,'FM1','Frýdek-Místek 1')");
        jdbc.update("INSERT INTO products(id, sku, name) VALUES (1,'A','Produkt A')");
        jdbc.update("INSERT INTO product_variations(id, product_id, location_id, code) VALUES (10,1,1,'A@FM1')");
        jdbc.update("INSERT INTO boxes(id, location_id, product_variation_id, box_no, state) VALUES (101,1,10,1,'AVAILABLE')");
    }

    @Test
    void open_ok_returns_boxNo() throws Exception {
        var reserved = reservationService.reservePaidOrder(123, 1, 10, 1);
        String pin = reserved.getFirst().pin();

        mvc.perform(post("/api/v1/open")
                        .contentType(APPLICATION_JSON)
                        .content("""
          {"deviceId":"ESP-FM1-01","wooOrderId":123,"pin":"%s"}
        """.formatted(pin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.boxNo").value(1));
    }

    @Test
    void open_bad_pin_format_returns_400() throws Exception {
        mvc.perform(post("/api/v1/open")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"deviceId":"ESP","wooOrderId":1,"pin":"12AB"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("BAD_PIN_FORMAT"));
    }
}
