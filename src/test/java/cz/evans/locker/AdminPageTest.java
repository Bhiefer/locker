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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminPageTest {

    static Path dbFile;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        dbFile = Files.createTempFile("locker-admin-", ".db");
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbFile.toAbsolutePath());
        r.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        r.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired JdbcTemplate jdbc;
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
        jdbc.update("INSERT INTO boxes(id, location_id, product_variation_id, box_no, state) VALUES (101,1,10,1,'EMPTY')");
    }

    @Test
    void admin_boxes_page_renders() throws Exception {
        mvc.perform(get("/admin/boxes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Boxy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("FM1")));
    }
}
