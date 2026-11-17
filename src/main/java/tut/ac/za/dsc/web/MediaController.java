package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.service.CardService;
import tut.ac.za.dsc.service.QrCodes;
import tut.ac.za.dsc.service.StudentService;

/**
 * Card photos and QR codes.
 * A photo is shown to the student, to signed-in staff, or to someone holding the card's signed QR link.
 * A QR code is shown to the student and to signed-in staff.
 */
@Controller
public class MediaController {

    private final StudentService students;
    private final CardService cards;
    private final AppProperties props;

    public MediaController(StudentService students, CardService cards, AppProperties props) {
        this.students = students;
        this.cards = cards;
        this.props = props;
    }

    @GetMapping("/media/photo")
    public ResponseEntity<byte[]> photo(@RequestParam String sn, @RequestParam(required = false) Integer v,
                                        @RequestParam(required = false) String t) {
        AppUser me = Web.currentUser();
        boolean allowed = me != null && (me.isStaff() || sn.equals(me.getUsername())) || cards.tokenMatches(sn, v, t);
        if (!allowed) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Optional<byte[]> photo = students.photo(sn);
        if (photo.isPresent()) {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).contentType(MediaType.IMAGE_JPEG).body(photo.get());
        }
        String initials = students.find(sn).map(Student::getInitials).orElse("?");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.valueOf("image/svg+xml"))
                .body(placeholder(initials).getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/media/qr")
    public ResponseEntity<byte[]> qr(@RequestParam String sn, @RequestParam(defaultValue = "360") int size, HttpServletRequest req) {
        AppUser me = Web.currentUser();
        if (me == null || !(me.isStaff() || sn.equals(me.getUsername()))) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Optional<Student> s = students.find(sn);
        if (s.isEmpty()) return ResponseEntity.notFound().build();
        byte[] png = QrCodes.png(Web.baseUrl(props, req) + cards.verificationPath(s.get()), Math.max(160, Math.min(size, 1000)));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(png);
    }

    /** A neutral picture with the student's initials, for records without a photo. */
    static String placeholder(String initials) {
        return "<svg xmlns='http://www.w3.org/2000/svg' width='480' height='640' viewBox='0 0 480 640'>"
                + "<rect width='480' height='640' fill='#EEEBFB'/><circle cx='240' cy='250' r='110' fill='#D9D1FF'/>"
                + "<path d='M60 640c20-140 100-210 180-210s160 70 180 210z' fill='#D9D1FF'/>"
                + "<text x='240' y='275' font-family='Arial,Helvetica,sans-serif' font-size='84' font-weight='700' text-anchor='middle' fill='#4329B0'>"
                + initials.replaceAll("[^A-Za-z?]", "") + "</text></svg>";
    }
}
