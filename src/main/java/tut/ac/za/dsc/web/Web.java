package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.security.AppUser;

/** Small helpers shared by the controllers. */
public final class Web {

    private Web() {
    }

    /** The signed-in person, or null. */
    public static AppUser currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof AppUser u ? u : null;
    }

    public static String currentUsername() {
        AppUser u = currentUser();
        return u == null ? null : u.getUsername();
    }

    /** Absolute address of the site, e.g. http://localhost:8080 (APP_PUBLIC_BASE_URL wins when set). */
    public static String baseUrl(AppProperties props, HttpServletRequest req) {
        String configured = props.getPublicBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        return ServletUriComponentsBuilder.fromContextPath(req).build().toUriString();
    }

    public static String clientIp(HttpServletRequest req) {
        return req.getRemoteAddr(); // already the real client address when forward-headers-strategy is enabled
    }

    /** Redirect that still shows a flash message, for handlers that otherwise return files. */
    public static <T> org.springframework.http.ResponseEntity<T> redirectWithFlash(HttpServletRequest req,
            jakarta.servlet.http.HttpServletResponse res, String path, Flash flash) {
        String location = req.getContextPath() + path;
        org.springframework.web.servlet.FlashMap map = org.springframework.web.servlet.support.RequestContextUtils.getOutputFlashMap(req);
        map.put("flash", flash);
        org.springframework.web.servlet.support.RequestContextUtils.saveOutputFlashMap(location, req, res);
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.SEE_OTHER)
                .location(java.net.URI.create(location)).build();
    }

    public static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
