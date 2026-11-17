package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.service.CardService;
import tut.ac.za.dsc.service.VerificationOutcome;

/** Landing page, student sign-in page, the public card check and the "not allowed" page. */
@Controller
public class PublicController {

    private final CardService cards;
    private final AppProperties props;

    public PublicController(CardService cards, AppProperties props) {
        this.cards = cards;
        this.props = props;
    }

    @GetMapping({"/", "/home"})
    public String home() {
        return "landing";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, @RequestParam(required = false) String signedOut,
                        HttpSession session, Model model) {
        AppUser me = Web.currentUser();
        if (me != null && me.isStudent()) return "redirect:/student";
        if (error != null) {
            Object locked = session.getAttribute("loginMessage");
            session.removeAttribute("loginMessage");
            model.addAttribute("loginError", locked != null ? locked : "The student number or password is not correct.");
        }
        if (signedOut != null) model.addAttribute("flash", Flash.info("You have signed out."));
        return "login";
    }

    /**
     * The page a phone opens when anyone scans a card's QR code with an ordinary camera app.
     * Shows only what is needed to confirm identity: photo, name, number, campus, status and expiry.
     */
    @GetMapping("/verify")
    public String verify(@RequestParam(required = false) String sn, @RequestParam(required = false) Integer v,
                         @RequestParam(required = false) String t, HttpServletRequest req, Model model) {
        if (sn == null || t == null) return "verify_help";
        AppUser me = Web.currentUser();
        String who = me != null && me.isStaff() ? me.getUsername() : "Public";
        VerificationOutcome outcome = cards.verify(sn, v, t, "PUBLIC_LINK", who, null, Web.clientIp(req));
        model.addAttribute("outcome", outcome);
        if (outcome.getStudent() != null) {
            model.addAttribute("photoUrl", "/media/photo?sn=" + sn + "&v=" + (v == null ? 1 : v) + "&t=" + t);
        }
        return "verify";
    }

    @GetMapping("/denied")
    public String denied(HttpServletResponse res, Model model) {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("status", 403);
        return "error";
    }
}
