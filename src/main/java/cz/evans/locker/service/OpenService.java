package cz.evans.locker.service;

import cz.evans.locker.dao.AllocationDao;
import cz.evans.locker.dao.OrderDao;
import cz.evans.locker.service.dto.OpenResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static cz.evans.locker.service.dto.OpenResult.Result.DENY;
import static cz.evans.locker.service.dto.OpenResult.Result.OK;

@Service
public class OpenService {

    private final JdbcTemplate jdbc;
    private final AllocationDao allocationDao;
    private final OrderDao orderDao;

    public OpenService(JdbcTemplate jdbc, AllocationDao allocationDao, OrderDao orderDao) {
        this.jdbc = jdbc;
        this.allocationDao = allocationDao;
        this.orderDao = orderDao;
    }

    @Transactional
    public OpenResult openByWooOrderAndPin(long wooOrderId, String pin) {
        // najdeme aktivní allocations
        List<AllocationDao.AllocationRow> rows = allocationDao.findActiveAllocationsByWooOrderId(wooOrderId);
        if (rows.isEmpty()) {
            // může být vyzvednuto, expirováno, nebo neexistuje
            return new OpenResult(DENY, null, "NOT_FOUND_OR_INACTIVE", null);
        }

        // expirace na úrovni order
        Instant expiresAt = Instant.parse(rows.getFirst().expiresAt());
        if (Instant.now().isAfter(expiresAt)) {
            orderDao.markExpired(rows.getFirst().orderId());
            return new OpenResult(DENY, null, "EXPIRED", null);
        }

        // hledáme shodu PINu mezi allocation řádky
        for (var r : rows) {
            if (r.failedAttempts() >= r.maxAttempts()) {
                return new OpenResult(DENY, null, "LOCKED", 0);
            }

            if (r.pinCode().equals(pin)) {
                // OK -> otevřít právě tento box
                allocationDao.markOpened(r.id());
                jdbc.update("UPDATE boxes SET state='EMPTY', last_change_at=datetime('now') WHERE id = ?", r.boxId());
                orderDao.markPickedUpIfAllOpened(r.orderId());

                Integer boxNo = jdbc.queryForObject("SELECT box_no FROM boxes WHERE id = ?", Integer.class, r.boxId());
                return new OpenResult(OK, boxNo, null, null);
            }
        }

        // PIN nesedí -> inkrement na prvním allocation (zjednodušení MVP)
        // (lepší: inkrementovat "globální pokusy na objednávku", ale pro MVP stačí)
        var first = rows.getFirst();
        allocationDao.incrementFailedAttempt(first.id());
        int remaining = Math.max(0, first.maxAttempts() - (first.failedAttempts() + 1));
        return new OpenResult(DENY, null, "INVALID_PIN", remaining);
    }
}
