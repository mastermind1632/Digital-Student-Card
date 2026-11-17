package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.service.MailService;

/** Values every page can use: settings, the current path and the signed-in person. */
@ControllerAdvice
public class GlobalModel {

    private final AppProperties props;
    private final MailService mail;

    public GlobalModel(AppProperties props, MailService mail) {
        this.props = props;
        this.mail = mail;
    }

    @ModelAttribute("cfg")
    public AppProperties cfg() {
        return props;
    }

    @ModelAttribute("mailEnabled")
    public boolean mailEnabled() {
        return mail.isEnabled();
    }

    @ModelAttribute("path")
    public String path(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.substring(req.getContextPath().length());
    }

    @ModelAttribute("me")
    public AppUser me() {
        return Web.currentUser();
    }

    /** Field errors after a failed form; empty unless a controller replaces it. */
    @ModelAttribute("errors")
    public Map<String, String> errors() {
        return Map.of();
    }
}
