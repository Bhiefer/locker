package cz.evans.locker.api;

import cz.evans.locker.service.OpenService;
import cz.evans.locker.service.dto.OpenRequest;
import cz.evans.locker.service.dto.OpenResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OpenController {

    private final OpenService openService;

    public OpenController(OpenService openService) {
        this.openService = openService;
    }

    @PostMapping("/open")
    public ResponseEntity<OpenResult> open(@RequestBody OpenRequest req) {
        // MVP validace (minimální)
        if (req.pin() == null || !req.pin().matches("\\d{4}")) {
            return ResponseEntity.badRequest()
                    .body(new OpenResult(OpenResult.Result.DENY, null, "BAD_PIN_FORMAT", null));
        }

        OpenResult result = openService.openByWooOrderAndPin(req.wooOrderId(), req.pin());
        return ResponseEntity.ok(result);
    }
}
