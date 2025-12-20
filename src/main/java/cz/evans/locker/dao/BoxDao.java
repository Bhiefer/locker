package cz.evans.locker.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BoxDao {
    private final JdbcTemplate jdbc;

    public BoxDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Long> findAvailableBoxIds(long productVariationId, int limit) {
        return jdbc.queryForList("""
      SELECT id
      FROM boxes
      WHERE product_variation_id = ?
        AND state = 'AVAILABLE'
      ORDER BY box_no
      LIMIT ?
    """, Long.class, productVariationId, limit);
    }

    public void markBoxesReserved(List<Long> boxIds) {
        if (boxIds.isEmpty()) return;
        String in = boxIds.stream().map(String::valueOf).reduce((a,b)->a+","+b).orElseThrow();
        jdbc.update("""
      UPDATE boxes
      SET state='RESERVED', last_change_at=datetime('now')
      WHERE id IN (%s)
    """.formatted(in));
    }

    public void markBoxEmpty(long boxId) {
        jdbc.update("""
      UPDATE boxes
      SET state='EMPTY', last_change_at=datetime('now')
      WHERE id = ?
    """, boxId);
    }
}
