package cz.evans.locker.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AllocationDao {
    private final JdbcTemplate jdbc;

    public AllocationDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insertAllocation(long orderId, long boxId, String pinCode) {
        jdbc.update("""
      INSERT INTO allocations (order_id, box_id, pin_code)
      VALUES (?, ?, ?)
    """, orderId, boxId, pinCode);
    }

    public List<AllocationRow> findActiveAllocationsByWooOrderId(long wooOrderId) {
        return jdbc.query("""
      SELECT a.id, a.order_id, a.box_id, a.pin_code, a.failed_attempts, a.max_attempts,
             o.status, o.expires_at
      FROM allocations a
      JOIN orders o ON o.id = a.order_id
      WHERE o.woo_order_id = ?
        AND o.status = 'PAID_RESERVED'
        AND a.opened_at IS NULL
    """, (rs, rowNum) -> new AllocationRow(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getLong("box_id"),
                rs.getString("pin_code"),
                rs.getInt("failed_attempts"),
                rs.getInt("max_attempts"),
                rs.getString("status"),
                rs.getString("expires_at")
        ), wooOrderId);
    }

    public void incrementFailedAttempt(long allocationId) {
        jdbc.update("""
      UPDATE allocations
      SET failed_attempts = failed_attempts + 1
      WHERE id = ?
    """, allocationId);
    }

    public void markOpened(long allocationId) {
        jdbc.update("""
      UPDATE allocations
      SET opened_at = datetime('now')
      WHERE id = ?
    """, allocationId);
    }

    public record AllocationRow(
            long id,
            long orderId,
            long boxId,
            String pinCode,
            int failedAttempts,
            int maxAttempts,
            String orderStatus,
            String expiresAt
    ) {}
}
