package cz.evans.locker;

import cz.evans.locker.service.OpenService;
import cz.evans.locker.service.ReservationService;
import cz.evans.locker.service.dto.OpenResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReservationAndOpenFlowTest {

    static Path dbFile;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        dbFile = Files.createTempFile("locker-test-", ".db");
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbFile.toAbsolutePath());
        r.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        r.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationService reservationService;
    @Autowired OpenService openService;

    @BeforeEach
    void seed() {
        // Vyčistit a znovu seedovat (pro jednoduchost)
        jdbc.update("DELETE FROM allocations");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM boxes");
        jdbc.update("DELETE FROM product_variations");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM locations");

        jdbc.update("INSERT INTO locations(id, code, name) VALUES (1,'FM1','Frýdek-Místek 1')");
        jdbc.update("INSERT INTO products(id, sku, name) VALUES (1,'A','Produkt A')");
        jdbc.update("INSERT INTO product_variations(id, product_id, location_id, code) VALUES (10,1,1,'A@FM1')");

        // 2 boxy pro A@FM1, oba AVAILABLE
        jdbc.update("INSERT INTO boxes(id, location_id, product_variation_id, box_no, state) VALUES (101,1,10,1,'AVAILABLE')");
        jdbc.update("INSERT INTO boxes(id, location_id, product_variation_id, box_no, state) VALUES (102,1,10,2,'AVAILABLE')");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void reserve_two_boxes_success() {
        var reserved = reservationService.reservePaidOrder(555, 1, 10, 2);

        assertEquals(2, reserved.size());
        assertTrue(reserved.get(0).pin().matches("\\d{4}"));
        assertTrue(reserved.get(1).pin().matches("\\d{4}"));

        Integer reservedCount = jdbc.queryForObject("""
      SELECT COUNT(*) FROM boxes WHERE product_variation_id=10 AND state='RESERVED'
    """, Integer.class);

        assertEquals(2, reservedCount);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void reserve_insufficient_boxes_fails() {
        assertThrows(IllegalStateException.class, () ->
                reservationService.reservePaidOrder(556, 1, 10, 3)
        );
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void open_with_correct_pin_marks_box_empty() {
        var reserved = reservationService.reservePaidOrder(777, 1, 10, 1);
        String pin = reserved.getFirst().pin();
        int boxNo = reserved.getFirst().boxNo();

        OpenResult r = openService.openByWooOrderAndPin(777, pin);
        assertEquals(OpenResult.Result.OK, r.result());
        assertEquals(boxNo, r.boxNo());

        Integer emptyCount = jdbc.queryForObject("""
      SELECT COUNT(*) FROM boxes WHERE state='EMPTY'
    """, Integer.class);
        assertEquals(1, emptyCount);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void open_with_wrong_pin_increments_attempts() {
        reservationService.reservePaidOrder(888, 1, 10, 1);

        OpenResult r = openService.openByWooOrderAndPin(888, "9999");
        assertEquals(OpenResult.Result.DENY, r.result());
        assertEquals("INVALID_PIN", r.reason());
        assertNotNull(r.remainingAttempts());

        Integer attempts = jdbc.queryForObject("""
      SELECT failed_attempts FROM allocations a
      JOIN orders o ON o.id = a.order_id
      WHERE o.woo_order_id = 888
    """, Integer.class);

        assertEquals(1, attempts);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void open_expired_denied() {
        // ručně vytvoříme expirovanou objednávku + allocation
        jdbc.update("""
      INSERT INTO orders(woo_order_id, location_id, status, expires_at)
      VALUES (999, 1, 'PAID_RESERVED', ?)
    """, Instant.now().minusSeconds(60).toString());

        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE woo_order_id=999", Long.class);

        jdbc.update("UPDATE boxes SET state='RESERVED' WHERE id=101");
        jdbc.update("INSERT INTO allocations(order_id, box_id, pin_code) VALUES (?,?,?)", orderId, 101, "1234");

        OpenResult r = openService.openByWooOrderAndPin(999, "1234");
        assertEquals(OpenResult.Result.DENY, r.result());
        assertEquals("EXPIRED", r.reason());

        String status = jdbc.queryForObject("SELECT status FROM orders WHERE woo_order_id=999", String.class);
        assertEquals("EXPIRED", status);
    }
}
