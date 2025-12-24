package cz.evans.locker.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class LocationDao {

    private final JdbcTemplate jdbc;

    public LocationDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<LocationRow> findByCode(String code) {
        return jdbc.query("""
      SELECT id, code, name,
             service_pin_code,
             service_pin_failed_attempts,
             service_pin_max_attempts,
             service_pin_locked_until
      FROM locations
      WHERE code = ?
    """, rs -> rs.next()
                        ? Optional.of(new LocationRow(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("service_pin_code"),
                        rs.getInt("service_pin_failed_attempts"),
                        rs.getInt("service_pin_max_attempts"),
                        rs.getString("service_pin_locked_until")
                ))
                        : Optional.empty()
                , code);
    }

    public void resetServicePinAttempts(long locationId) {
        jdbc.update("""
      UPDATE locations
      SET service_pin_failed_attempts = 0,
          service_pin_locked_until = NULL
      WHERE id = ?
    """, locationId);
    }

    public void incrementServicePinAttempts(long locationId) {
        jdbc.update("""
      UPDATE locations
      SET service_pin_failed_attempts = service_pin_failed_attempts + 1
      WHERE id = ?
    """, locationId);
    }

    public void lockServicePin(long locationId, Instant lockedUntil) {
        jdbc.update("""
      UPDATE locations
      SET service_pin_locked_until = ?
      WHERE id = ?
    """, lockedUntil.toString(), locationId);
    }

    public record LocationRow(
            long id,
            String code,
            String name,
            String servicePinCode,
            int servicePinFailedAttempts,
            int servicePinMaxAttempts,
            String servicePinLockedUntil
    ) {}
}
