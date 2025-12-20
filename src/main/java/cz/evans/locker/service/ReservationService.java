package cz.evans.locker.service;

import cz.evans.locker.dao.AllocationDao;
import cz.evans.locker.dao.BoxDao;
import cz.evans.locker.dao.OrderDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    private final JdbcTemplate jdbc;
    private final BoxDao boxDao;
    private final OrderDao orderDao;
    private final AllocationDao allocationDao;

    private final SecureRandom rnd = new SecureRandom();

    public ReservationService(JdbcTemplate jdbc, BoxDao boxDao, OrderDao orderDao, AllocationDao allocationDao) {
        this.jdbc = jdbc;
        this.boxDao = boxDao;
        this.orderDao = orderDao;
        this.allocationDao = allocationDao;
    }

    /** Vytvoří rezervaci pro zaplacenou objednávku: qty boxů pro konkrétní variantu. */
    @Transactional
    public List<ReservedBox> reservePaidOrder(long wooOrderId, long locationId, long productVariationId, int qty) {
        // SQLite: zlepší chování při souběhu
//        jdbc.execute("BEGIN IMMEDIATE");

        Instant expiresAt = Instant.now().plus(Duration.ofDays(3));
        long orderId = orderDao.insertOrder(wooOrderId, locationId, expiresAt);

        List<Long> boxIds = boxDao.findAvailableBoxIds(productVariationId, qty);
        if (boxIds.size() < qty) {
            throw new IllegalStateException("Nedostatek AVAILABLE boxů pro qty=" + qty);
        }

        boxDao.markBoxesReserved(boxIds);

        List<ReservedBox> out = new ArrayList<>();
        for (Long boxId : boxIds) {
            String pin = generate4DigitPin();
            allocationDao.insertAllocation(orderId, boxId, pin);
            int boxNo = jdbc.queryForObject("SELECT box_no FROM boxes WHERE id = ?", Integer.class, boxId);
            out.add(new ReservedBox(boxId, boxNo, pin));
        }

        return out;
    }

    private String generate4DigitPin() {
        int v = rnd.nextInt(10_000); // 0..9999
        return String.format("%04d", v);
    }

    public record ReservedBox(long boxId, int boxNo, String pin) {}
}
