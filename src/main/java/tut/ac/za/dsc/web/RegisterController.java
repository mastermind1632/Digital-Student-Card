package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.security.SessionLogin;
import tut.ac.za.dsc.service.RegistrationForm;
import tut.ac.za.dsc.service.StudentService;
import tut.ac.za.dsc.service.ValidationException;

/** Self-registration for new students. */
@Controller
public class RegisterController {

    private final StudentService students;
    private final SessionLogin sessionLogin;
    private final AppProperties props;

    public RegisterController(StudentService students, SessionLogin sessionLogin, AppProperties props) {
        this.students = students;
        this.sessionLogin = sessionLogin;
        this.props = props;
    }

    @GetMapping("/register")
    public String form(Model model) {
        model.addAttribute("form", new RegistrationForm());
        model.addAttribute("needsApproval", props.isRegistrationApproval());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegistrationForm form, @RequestParam(required = false) MultipartFile photo,
                           HttpServletRequest req, HttpServletResponse res, Model model, RedirectAttributes flash) throws IOException {
        try {
            Student s = students.register(form, bytes(photo), false, null);
            sessionLogin.signIn(new AppUser(AppUser.Kind.STUDENT, s.getStudentNumber(), s.getFullName(), null, true), req, res);
            flash.addFlashAttribute("justRegistered", true);
            return "redirect:/student";
        } catch (ValidationException e) {
            form.setPassword(null);
            form.setConfirmPassword(null);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("needsApproval", props.isRegistrationApproval());
            res.setStatus(422);
            return "register";
        }
    }

    static byte[] bytes(MultipartFile f) throws IOException {
        return f == null || f.isEmpty() ? null : f.getBytes();
    }
}
