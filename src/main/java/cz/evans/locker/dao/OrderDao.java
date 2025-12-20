package cz.evans.locker.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Optional;

@Repository
public class OrderDao {
    private final JdbcTemplate jdbc;

    public OrderDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insertOrder(long wooOrderId, long locationId, Instant expiresAt) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
        INSERT INTO orders (woo_order_id, location_id, status, expires_at)
        VALUES (?, ?, 'PAID_RESERVED', ?)
      """, new String[]{"id"});
            ps.setLong(1, wooOrderId);
            ps.setLong(2, locationId);
            ps.setString(3, expiresAt.toString());
            return ps;
        }, kh);

        Number key = kh.getKey();
        if (key == null) throw new IllegalStateException("Order insert failed (no key).");
        return key.longValue();
    }

    public Optional<OrderRow> findByWooOrderId(long wooOrderId) {
        return jdbc.query("""
      SELECT id, woo_order_id, location_id, status, expires_at
      FROM orders
      WHERE woo_order_id = ?
    """, (rs) -> rs.next()
                        ? Optional.of(new OrderRow(
                        rs.getLong("id"),
                        rs.getLong("woo_order_id"),
                        rs.getLong("location_id"),
                        rs.getString("status"),
                        rs.getString("expires_at")
                ))
                        : Optional.empty()
                , wooOrderId);
    }

    public void markPickedUpIfAllOpened(long orderId) {
        Integer remaining = jdbc.queryForObject("""
      SELECT COUNT(*)
      FROM allocations
      WHERE order_id = ? AND opened_at IS NULL
    """, Integer.class, orderId);

        if (remaining != null && remaining == 0) {
            jdbc.update("""
        UPDATE orders
        SET status='PICKED_UP', picked_up_at=datetime('now')
        WHERE id = ? AND status='PAID_RESERVED'
      """, orderId);
        }
    }

    public void markExpired(long orderId) {
        jdbc.update("""
      UPDATE orders
      SET status='EXPIRED'
      WHERE id = ? AND status='PAID_RESERVED'
    """, orderId);
    }

    public record OrderRow(long id, long wooOrderId, long locationId, String status, String expiresAt) {}
}
