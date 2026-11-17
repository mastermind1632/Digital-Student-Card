package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.service.CardService;
import tut.ac.za.dsc.service.VerificationOutcome;

/**
 * Gate scanner for staff: the page reads card QR codes with the camera and asks /scan/check about each one
 * without reloading. A student number can also be typed in.
 */
@Controller
public class ScanController {

    private final CardService cards;

    public ScanController(CardService cards) {
        this.cards = cards;
    }

    @GetMapping("/scan")
    public String scan() {
        return "scan";
    }

    @PostMapping("/scan/check")
    @ResponseBody
    public Map<String, Object> check(@RequestParam String code, @RequestParam(required = false) String location,
                                     @RequestParam(defaultValue = "false") boolean typed, HttpServletRequest req) {
        String sn;
        Integer version = null;
        String token = null;
        String method;
        if (code.contains("sn=")) {
            Map<String, String> q = query(code);
            sn = q.get("sn");
            token = q.getOrDefault("t", "");
            try {
                version = q.get("v") == null ? null : Integer.valueOf(q.get("v"));
            } catch (NumberFormatException e) {
                version = null;
            }
            method = "SCANNER";
        } else {
            sn = code.replaceAll("\\s", ""); // typed number, or an old card whose QR held only the number
            method = typed ? "MANUAL" : "SCANNER";
        }
        AppUser me = Web.currentUser();
        VerificationOutcome o = cards.verify(sn, version, token, method, me == null ? null : me.getUsername(), location, Web.clientIp(req));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("result", o.getResult().name());
        out.put("tone", o.getTone());
        out.put("headline", o.getHeadline());
        out.put("advice", o.getAdvice());
        out.put("checkedAt", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        Student s = o.getStudent();
        if (s != null) {
            Map<String, Object> st = new LinkedHashMap<>();
            st.put("name", s.getFullName());
            st.put("number", s.getStudentNumberDisplay());
            st.put("campus", s.getCampus());
            st.put("qualification", s.getQualification());
            st.put("expiry", s.getExpiryDateText());
            st.put("status", s.getStatusLabel());
            st.put("reason", s.getStatusReason());
            st.put("photo", req.getContextPath() + "/media/photo?sn=" + s.getStudentNumber());
            if (me != null && me.isAdmin()) st.put("profile", req.getContextPath() + "/admin/student?sn=" + s.getStudentNumber());
            out.put("student", st);
        }
        return out;
    }

    private static Map<String, String> query(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        try {
            String url = text.contains("?") ? text : "/?" + text;
            UriComponentsBuilder.fromUri(URI.create(url.trim())).build().getQueryParams()
                    .forEach((k, v) -> m.put(k, v.isEmpty() ? null : v.get(0)));
        } catch (IllegalArgumentException e) {
            // not a link from this system: leave empty so it is reported as not found
        }
        return m;
    }
}
