package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.StaffUser;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.security.SessionLogin;
import tut.ac.za.dsc.service.StaffService;
import tut.ac.za.dsc.service.ValidationException;

/** Staff sign-in page and the one-time creation of the first administrator. */
@Controller
public class StaffLoginController {

    private final StaffService staff;
    private final SessionLogin sessionLogin;
    private final AppProperties props;

    public StaffLoginController(StaffService staff, SessionLogin sessionLogin, AppProperties props) {
        this.staff = staff;
        this.sessionLogin = sessionLogin;
        this.props = props;
    }

    @GetMapping("/admin/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String signedOut,
                        HttpSession session, Model model) {
        if (staff.noStaffYet()) return "redirect:/admin/setup";
        AppUser me = Web.currentUser();
        if (me != null && me.isStaff()) return me.isAdmin() ? "redirect:/admin" : "redirect:/scan";
        if (error != null) {
            Object locked = session.getAttribute("loginMessage");
            session.removeAttribute("loginMessage");
            model.addAttribute("loginError", locked != null ? locked
                    : "disabled".equals(error) ? "This account is switched off. Ask an administrator."
                    : "The username or password is not correct.");
        }
        if (signedOut != null) model.addAttribute("flash", Flash.info("You have signed out."));
        return "admin/login";
    }

    @GetMapping("/admin/setup")
    public String setup(Model model) {
        if (!staff.noStaffYet()) return "redirect:/admin/login";
        model.addAttribute("form", Map.of());
        model.addAttribute("needsKey", !props.getSetupKey().isBlank());
        return "admin/setup";
    }

    @PostMapping("/admin/setup")
    public String createFirstAdmin(@RequestParam(required = false) String fullName, @RequestParam(required = false) String username,
                                   @RequestParam(required = false) String password, @RequestParam(required = false) String confirmPassword,
                                   @RequestParam(required = false) String setupKey, HttpServletRequest req, HttpServletResponse res,
                                   Model model, RedirectAttributes flash) {
        if (!staff.noStaffYet()) return "redirect:/admin/login";
        try {
            if (!props.getSetupKey().isBlank() && !props.getSetupKey().equals(setupKey)) {
                throw ValidationException.of("setupKey", "The setup key is not correct.");
            }
            StaffUser u = staff.create(username, fullName, password, confirmPassword, "ADMIN", "setup");
            sessionLogin.signIn(new AppUser(AppUser.Kind.ADMIN, u.getUsername(), u.getFullName(), null, true), req, res);
            flash.addFlashAttribute("flash", Flash.success("Your administrator account is ready. Next, add security officers under Staff accounts."));
            return "redirect:/admin";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("form", Map.of("fullName", fullName == null ? "" : fullName, "username", username == null ? "" : username));
            model.addAttribute("needsKey", !props.getSetupKey().isBlank());
            res.setStatus(422);
            return "admin/setup";
        }
    }
}
