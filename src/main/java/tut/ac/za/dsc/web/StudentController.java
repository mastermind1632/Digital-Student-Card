package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.CardStatus;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.service.CardPdf;
import tut.ac.za.dsc.service.CardService;
import tut.ac.za.dsc.service.DirectoryService;
import tut.ac.za.dsc.service.MailService;
import tut.ac.za.dsc.service.StudentService;
import tut.ac.za.dsc.service.ValidationException;

/** Everything a signed-in student can do with his or her own card and details. */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService students;
    private final CardService cards;
    private final DirectoryService directory;
    private final MailService mail;
    private final AppProperties props;

    public StudentController(StudentService students, CardService cards, DirectoryService directory, MailService mail, AppProperties props) {
        this.students = students;
        this.cards = cards;
        this.directory = directory;
        this.mail = mail;
        this.props = props;
    }

    /** The signed-in student, added to every page of this controller. */
    @ModelAttribute("student")
    public Student student() {
        return students.require(Web.currentUsername());
    }

    @ModelAttribute("verifyUrl")
    public String verifyUrl(@ModelAttribute(name = "student", binding = false) Student s, HttpServletRequest req) {
        return Web.baseUrl(props, req) + cards.verificationPath(s);
    }

    @GetMapping({"", "/"})
    public String dashboard(@ModelAttribute(name = "student", binding = false) Student s, Model model) {
        model.addAttribute("checks", directory.checksFor(s.getStudentNumber(), 5));
        model.addAttribute("checkCount", directory.countChecksFor(s.getStudentNumber()));
        return "student/dashboard";
    }

    @GetMapping("/card")
    public String card() {
        return "student/card";
    }

    @GetMapping("/activity")
    public String activity(@ModelAttribute(name = "student", binding = false) Student s, Model model) {
        model.addAttribute("checks", directory.checksFor(s.getStudentNumber(), 100));
        return "student/activity";
    }

    @GetMapping("/profile")
    public String profile(@ModelAttribute(name = "student", binding = false) Student s, Model model) {
        model.addAttribute("form", Map.of("email", s.getEmail(), "phoneNumber", s.getPhoneNumber() == null ? "" : s.getPhoneNumber()));
        model.addAttribute("canChangePhoto", s.getEffectiveStatus() == CardStatus.PENDING);
        return "student/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@ModelAttribute(name = "student", binding = false) Student s, @RequestParam(required = false) String email,
                              @RequestParam(required = false) String phoneNumber, Model model, HttpServletResponse res,
                              RedirectAttributes flash) {
        try {
            students.updateContactDetails(s.getStudentNumber(), email, phoneNumber);
            flash.addFlashAttribute("flash", Flash.success("Your contact details were saved."));
            return "redirect:/student/profile";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("form", Map.of("email", email == null ? "" : email, "phoneNumber", phoneNumber == null ? "" : phoneNumber));
            model.addAttribute("canChangePhoto", s.getEffectiveStatus() == CardStatus.PENDING);
            res.setStatus(422);
            return "student/profile";
        }
    }

    /** Students can replace the photo only while the card waits for approval; after that only the card office can. */
    @PostMapping("/photo")
    public String photo(@ModelAttribute(name = "student", binding = false) Student s, @RequestParam(required = false) MultipartFile photo,
                        RedirectAttributes flash) throws IOException {
        if (s.getEffectiveStatus() != CardStatus.PENDING) {
            flash.addFlashAttribute("flash", Flash.warning("Your card has been issued, so your photo can only be changed at " + props.getCardOffice() + "."));
            return "redirect:/student/profile";
        }
        try {
            students.changePhoto(s.getStudentNumber(), RegisterController.bytes(photo), s.getStudentNumber());
            flash.addFlashAttribute("flash", Flash.success("Your new photo was saved."));
        } catch (ValidationException e) {
            flash.addFlashAttribute("flash", Flash.error(e.getMessage()));
        }
        return "redirect:/student/profile";
    }

    @GetMapping("/password")
    public String password() {
        return "student/password";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute(name = "student", binding = false) Student s, @RequestParam(required = false) String currentPassword,
                                 @RequestParam(required = false) String newPassword, @RequestParam(required = false) String confirmPassword,
                                 Model model, HttpServletResponse res, RedirectAttributes flash) {
        try {
            students.changePassword(s.getStudentNumber(), currentPassword, newPassword, confirmPassword);
            flash.addFlashAttribute("flash", Flash.success("Your password was changed."));
            return "redirect:/student";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            res.setStatus(422);
            return "student/password";
        }
    }

    @GetMapping("/email")
    public String emailPage() {
        return "student/email";
    }

    @PostMapping("/email")
    public String email(@ModelAttribute(name = "student", binding = false) Student s, @ModelAttribute(name = "verifyUrl", binding = false) String verifyUrl, Model model,
                        RedirectAttributes flash) throws Exception {
        if (!s.isCardValid()) {
            flash.addFlashAttribute("flash", Flash.warning("You can email your card once it is valid."));
            return "redirect:/student";
        }
        String problem = mail.sendCard(s, pdf(s, verifyUrl), verifyUrl);
        if (problem != null) {
            model.addAttribute("emailError", problem);
            return "student/email";
        }
        flash.addFlashAttribute("flash", Flash.success("Your card was sent to " + s.getEmail() + "."));
        return "redirect:/student";
    }

    @GetMapping("/card.pdf")
    public ResponseEntity<byte[]> cardPdf(@ModelAttribute(name = "student", binding = false) Student s,
                                          @ModelAttribute(name = "verifyUrl", binding = false) String verifyUrl,
                                          HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (!s.isCardValid()) {
            return Web.redirectWithFlash(req, res, "/student",
                    Flash.warning("You can download your card once it is valid. Status: " + s.getStatusLabel() + "."));
        }
        return pdfResponse(s, pdf(s, verifyUrl));
    }

    private byte[] pdf(Student s, String verifyUrl) throws Exception {
        return CardPdf.build(s, students.photo(s.getStudentNumber()).orElse(null), verifyUrl, props.getInstitutionName(), props.getCardOffice());
    }

    static ResponseEntity<byte[]> pdfResponse(Student s, byte[] pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("student-card-" + s.getStudentNumber() + ".pdf").build().toString())
                .body(pdf);
    }
}
