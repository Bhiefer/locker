package cz.evans.locker.service;

import cz.evans.locker.dao.LocationDao;
import cz.evans.locker.service.dto.ServiceOpenResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static cz.evans.locker.service.dto.ServiceOpenResult.Result.DENY;
import static cz.evans.locker.service.dto.ServiceOpenResult.Result.OK;

@Service
public class ServiceOpenService {

    private final LocationDao locationDao;

    // MVP: po locku čekat 15 minut
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public ServiceOpenService(LocationDao locationDao) {
        this.locationDao = locationDao;
    }

    @Transactional
    public ServiceOpenResult authorizeServiceOpen(String locationCode, String servicePin, int boxNo) {
        var locOpt = locationDao.findByCode(locationCode);
        if (locOpt.isEmpty()) {
            return new ServiceOpenResult(DENY, null, "UNKNOWN_LOCATION");
        }

        var loc = locOpt.get();

        if (servicePin == null || !servicePin.matches("\\d{6}")) {
            return new ServiceOpenResult(DENY, null, "BAD_SERVICE_PIN_FORMAT");
        }

        // Lock kontrola
        if (loc.servicePinLockedUntil() != null && !loc.servicePinLockedUntil().isBlank()) {
            Instant lockedUntil = Instant.parse(loc.servicePinLockedUntil());
            if (Instant.now().isBefore(lockedUntil)) {
                return new ServiceOpenResult(DENY, null, "LOCKED");
            }
        }

        // Není nastaven servisní PIN
        if (loc.servicePinCode() == null || loc.servicePinCode().isBlank()) {
            return new ServiceOpenResult(DENY, null, "SERVICE_PIN_NOT_SET");
        }

        // Ověření PINu
        if (!loc.servicePinCode().equals(servicePin)) {
            locationDao.incrementServicePinAttempts(loc.id());

            int attemptsAfter = loc.servicePinFailedAttempts() + 1;
            if (attemptsAfter >= loc.servicePinMaxAttempts()) {
                locationDao.lockServicePin(loc.id(), Instant.now().plus(LOCK_DURATION));
                return new ServiceOpenResult(DENY, null, "LOCKED");
            }

            return new ServiceOpenResult(DENY, null, "INVALID_SERVICE_PIN");
        }

        // OK: reset pokusů a autorizace otevření boxu
        locationDao.resetServicePinAttempts(loc.id());
        return new ServiceOpenResult(OK, boxNo, null);
    }
}
