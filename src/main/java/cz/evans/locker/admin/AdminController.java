package cz.evans.locker.admin;

import cz.evans.locker.dao.AdminBoxDao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminBoxDao adminBoxDao;

    public AdminController(AdminBoxDao adminBoxDao) {
        this.adminBoxDao = adminBoxDao;
    }

    @GetMapping("/boxes")
    public String boxes(@RequestParam(required = false) Long locationId, Model model) {
        model.addAttribute("boxes", adminBoxDao.listBoxes(locationId));
        model.addAttribute("locationId", locationId);
        return "boxes";
    }

    @PostMapping("/boxes/{boxId}/mark-filled")
    public String markFilled(@PathVariable long boxId, @RequestParam(required = false) Long locationId) {
        adminBoxDao.setState(boxId, "AVAILABLE");
        return redirect(locationId);
    }

    @PostMapping("/boxes/{boxId}/mark-empty")
    public String markEmpty(@PathVariable long boxId, @RequestParam(required = false) Long locationId) {
        adminBoxDao.setState(boxId, "EMPTY");
        return redirect(locationId);
    }

    @PostMapping("/boxes/{boxId}/mark-out")
    public String markOut(@PathVariable long boxId, @RequestParam(required = false) Long locationId) {
        adminBoxDao.setState(boxId, "OUT_OF_SERVICE");
        return redirect(locationId);
    }

    private String redirect(Long locationId) {
        return (locationId == null) ? "redirect:/admin/boxes" : "redirect:/admin/boxes?locationId=" + locationId;
    }
}
