package cz.evans.locker.api;

import cz.evans.locker.service.ServiceOpenService;
import cz.evans.locker.service.dto.ServiceOpenRequest;
import cz.evans.locker.service.dto.ServiceOpenResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/service")
public class ServiceOpenController {

    private final ServiceOpenService serviceOpenService;

    public ServiceOpenController(ServiceOpenService serviceOpenService) {
        this.serviceOpenService = serviceOpenService;
    }

    @PostMapping("/open")
    public ResponseEntity<ServiceOpenResult> open(@RequestBody ServiceOpenRequest req) {
        if (req.locationCode() == null || req.locationCode().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ServiceOpenResult(ServiceOpenResult.Result.DENY, null, "BAD_LOCATION"));
        }
        if (req.boxNo() <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ServiceOpenResult(ServiceOpenResult.Result.DENY, null, "BAD_BOX_NO"));
        }

        ServiceOpenResult res = serviceOpenService.authorizeServiceOpen(
                req.locationCode().trim(),
                req.servicePin(),
                req.boxNo()
        );

        return ResponseEntity.ok(res);
    }
}
