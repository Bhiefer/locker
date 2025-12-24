package cz.evans.locker.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdminBoxDao {

    private final JdbcTemplate jdbc;

    public AdminBoxDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BoxViewRow> listBoxes(Long locationId) {
        // pro MVP: locationId může být null = všechny
        if (locationId == null) {
            return jdbc.query("""
        SELECT b.id, l.code AS location_code, b.box_no, b.state,
               pv.code AS variation_code, p.sku AS product_sku
        FROM boxes b
        JOIN locations l ON l.id = b.location_id
        JOIN product_variations pv ON pv.id = b.product_variation_id
        JOIN products p ON p.id = pv.product_id
        ORDER BY l.code, b.box_no
      """, (rs, i) -> new BoxViewRow(
                    rs.getLong("id"),
                    rs.getString("location_code"),
                    rs.getInt("box_no"),
                    rs.getString("state"),
                    rs.getString("product_sku"),
                    rs.getString("variation_code")
            ));
        }

        return jdbc.query("""
      SELECT b.id, l.code AS location_code, b.box_no, b.state,
             pv.code AS variation_code, p.sku AS product_sku
      FROM boxes b
      JOIN locations l ON l.id = b.location_id
      JOIN product_variations pv ON pv.id = b.product_variation_id
      JOIN products p ON p.id = pv.product_id
      WHERE b.location_id = ?
      ORDER BY b.box_no
    """, (rs, i) -> new BoxViewRow(
                rs.getLong("id"),
                rs.getString("location_code"),
                rs.getInt("box_no"),
                rs.getString("state"),
                rs.getString("product_sku"),
                rs.getString("variation_code")
        ), locationId);
    }

    public void setState(long boxId, String state) {
        jdbc.update("""
      UPDATE boxes
      SET state = ?, last_change_at = datetime('now')
      WHERE id = ?
    """, state, boxId);
    }

    public record BoxViewRow(
            long id,
            String locationCode,
            int boxNo,
            String state,
            String productSku,
            String variationCode
    ) {}
}
