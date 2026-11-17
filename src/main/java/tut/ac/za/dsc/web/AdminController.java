package tut.ac.za.dsc.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
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
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.service.AuditService;
import tut.ac.za.dsc.service.CardPdf;
import tut.ac.za.dsc.service.CardService;
import tut.ac.za.dsc.service.CsvExport;
import tut.ac.za.dsc.service.DirectoryService;
import tut.ac.za.dsc.service.MailService;
import tut.ac.za.dsc.service.RegistrationForm;
import tut.ac.za.dsc.service.StaffService;
import tut.ac.za.dsc.service.StudentQuery;
import tut.ac.za.dsc.service.StudentService;
import tut.ac.za.dsc.service.ValidationException;

/** The card office: overview, student directory, card actions, check history, activity log and staff. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final StudentService students;
    private final DirectoryService directory;
    private final CardService cards;
    private final StaffService staff;
    private final AuditService audit;
    private final MailService mail;
    private final AppProperties props;

    public AdminController(StudentService students, DirectoryService directory, CardService cards, StaffService staff,
                           AuditService audit, MailService mail, AppProperties props) {
        this.students = students;
        this.directory = directory;
        this.cards = cards;
        this.staff = staff;
        this.audit = audit;
        this.mail = mail;
        this.props = props;
    }

    /** Number shown next to "Students" in the menu. */
    @ModelAttribute("pendingCount")
    public long pendingCount() {
        return directory.countPending();
    }

    // ------------------------------------------------------------ overview

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("stats", directory.stats());
        model.addAttribute("pending", directory.pending(6));
        model.addAttribute("recentChecks", directory.recentChecks(8));
        model.addAttribute("recentEvents", audit.recent(8));
        return "admin/dashboard";
    }

    // ------------------------------------------------------------ students

    @GetMapping("/students")
    public String students(@ModelAttribute("query") StudentQuery query, Model model) {
        model.addAttribute("result", directory.search(query));
        model.addAttribute("filterQuery", query.toQueryString());
        return "admin/students";
    }

    @GetMapping("/students/export")
    public ResponseEntity<byte[]> export(@ModelAttribute("query") StudentQuery query) {
        List<Student> list = directory.searchAll(query);
        audit.log(Web.currentUsername(), "Student list exported", null, list.size() + " rows");
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("students-" + LocalDate.now() + ".csv").build().toString())
                .body(CsvExport.students(list).getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/students/bulk")
    public String bulk(@RequestParam(required = false) List<String> sn, @RequestParam String action,
                       @RequestParam(required = false) String back, RedirectAttributes flash) {
        if (sn == null || sn.isEmpty()) {
            flash.addFlashAttribute("flash", Flash.warning("Select at least one student first."));
        } else {
            int done = 0;
            for (String number : sn) {
                try {
                    if ("approve".equals(action)) cards.approve(number, Web.currentUsername());
                    else if ("renew".equals(action)) cards.renew(number, Web.currentUsername());
                    else continue;
                    done++;
                } catch (ValidationException ignored) {
                    // removed meanwhile; skip it
                }
            }
            flash.addFlashAttribute("flash", Flash.success(("approve".equals(action) ? "Approved " : "Renewed ") + done + " card" + (done == 1 ? "" : "s") + "."));
        }
        return "redirect:" + (back != null && back.startsWith("/admin") && !back.contains("//") ? back : "/admin/students");
    }

    @GetMapping("/students/new")
    public String newStudent(Model model) {
        model.addAttribute("form", new RegistrationForm());
        model.addAttribute("mode", "new");
        return "admin/student_form";
    }

    @PostMapping("/students/new")
    public String createStudent(@ModelAttribute("form") RegistrationForm form, @RequestParam(required = false) MultipartFile photo,
                                Model model, HttpServletResponse res, RedirectAttributes flash) throws IOException {
        try {
            Student s = students.register(form, RegisterController.bytes(photo), true, Web.currentUsername());
            flash.addFlashAttribute("flash", Flash.success(s.getFullName() + " is registered with student number " + s.getStudentNumber() + ". The card is valid."));
            return "redirect:/admin/student?sn=" + s.getStudentNumber();
        } catch (ValidationException e) {
            form.setPassword(null);
            form.setConfirmPassword(null);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("mode", "new");
            res.setStatus(422);
            return "admin/student_form";
        }
    }

    @GetMapping("/student")
    public String student(@RequestParam String sn, Model model, HttpServletRequest req) {
        Student s = students.require(sn);
        model.addAttribute("student", s);
        model.addAttribute("checks", directory.checksFor(sn, 10));
        model.addAttribute("events", audit.forTarget(sn, 15));
        model.addAttribute("verifyUrl", Web.baseUrl(props, req) + cards.verificationPath(s));
        return "admin/student";
    }

    @GetMapping("/student/edit")
    public String edit(@RequestParam String sn, Model model) {
        Student s = students.require(sn);
        RegistrationForm f = new RegistrationForm();
        f.setFullName(s.getFullName());
        f.setEmail(s.getEmail());
        f.setPhoneNumber(s.getPhoneNumber());
        f.setCampus(s.getCampus());
        f.setDepartment(s.getDepartment());
        f.setQualification(s.getQualification());
        f.setYearOfStudy(s.getYearOfStudy());
        model.addAttribute("student", s);
        model.addAttribute("form", f);
        model.addAttribute("mode", "edit");
        return "admin/student_form";
    }

    @PostMapping("/student/edit")
    public String saveEdit(@RequestParam String sn, @ModelAttribute("form") RegistrationForm form, Model model,
                           HttpServletResponse res, RedirectAttributes flash) {
        try {
            students.adminUpdate(sn, form, Web.currentUsername());
            flash.addFlashAttribute("flash", Flash.success("The changes were saved."));
            return "redirect:/admin/student?sn=" + sn;
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("student", students.require(sn));
            model.addAttribute("mode", "edit");
            res.setStatus(422);
            return "admin/student_form";
        }
    }

    @PostMapping("/student/action")
    public String action(@RequestParam String sn, @RequestParam String action, @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String confirm, @RequestParam(required = false) MultipartFile photo,
                         HttpServletRequest req, RedirectAttributes flash) throws Exception {
        String actor = Web.currentUsername();
        String back = "redirect:/admin/student?sn=" + sn;
        try {
            switch (action) {
                case "approve" -> {
                    cards.approve(sn, actor);
                    flash.addFlashAttribute("flash", Flash.success("The card is approved and now valid."));
                }
                case "suspend" -> {
                    cards.suspend(sn, reason, actor);
                    flash.addFlashAttribute("flash", Flash.success("The card is suspended. Scans will show it as not valid."));
                }
                case "revoke" -> {
                    cards.revoke(sn, reason, actor);
                    flash.addFlashAttribute("flash", Flash.success("The card is revoked."));
                }
                case "reactivate" -> {
                    cards.reactivate(sn, actor);
                    flash.addFlashAttribute("flash", Flash.success("The card is valid again."));
                }
                case "renew" -> {
                    Student s = cards.renew(sn, actor);
                    flash.addFlashAttribute("flash", Flash.success("The card is renewed until " + s.getExpiryDateText() + "."));
                }
                case "reissue" -> {
                    cards.reissue(sn, actor);
                    flash.addFlashAttribute("flash", Flash.success("A replacement card was issued. The QR code on the old card no longer works."));
                }
                case "reset-password" -> {
                    flash.addFlashAttribute("tempPassword", students.resetPassword(sn, actor));
                    flash.addFlashAttribute("flash", Flash.success("A temporary password was created. Give it to the student in person."));
                }
                case "photo" -> {
                    students.changePhoto(sn, RegisterController.bytes(photo), actor);
                    flash.addFlashAttribute("flash", Flash.success("The photo was replaced."));
                }
                case "email" -> {
                    Student s = students.require(sn);
                    if (!s.isCardValid()) {
                        flash.addFlashAttribute("flash", Flash.warning("Only valid cards can be emailed."));
                    } else {
                        String url = Web.baseUrl(props, req) + cards.verificationPath(s);
                        byte[] pdf = CardPdf.build(s, students.photo(sn).orElse(null), url, props.getInstitutionName(), props.getCardOffice());
                        String problem = mail.sendCard(s, pdf, url);
                        if (problem == null) {
                            audit.log(actor, "Card emailed", sn, s.getEmail());
                            flash.addFlashAttribute("flash", Flash.success("The card was emailed to " + s.getEmail() + "."));
                        } else {
                            flash.addFlashAttribute("flash", Flash.error(problem));
                        }
                    }
                }
                case "delete" -> {
                    students.delete(sn, confirm, actor);
                    flash.addFlashAttribute("flash", Flash.success("The student and card were deleted."));
                    back = "redirect:/admin/students";
                }
                default -> flash.addFlashAttribute("flash", Flash.error("Unknown action."));
            }
        } catch (ValidationException e) {
            flash.addFlashAttribute("flash", Flash.error(e.getMessage()));
        }
        return back;
    }

    @GetMapping("/student/card.pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam String sn, HttpServletRequest req) throws Exception {
        Student s = students.require(sn);
        byte[] pdf = CardPdf.build(s, students.photo(sn).orElse(null), Web.baseUrl(props, req) + cards.verificationPath(s),
                props.getInstitutionName(), props.getCardOffice());
        audit.log(Web.currentUsername(), "Card PDF downloaded", sn, null);
        return StudentController.pdfResponse(s, pdf);
    }

    // ------------------------------------------------------------ records

    @GetMapping("/checks")
    public String checks(@RequestParam(required = false) String q, @RequestParam(required = false) String result,
                         @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("result", directory.searchChecks(q, result, page));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("resultFilter", result == null ? "" : result);
        return "admin/checks";
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String q, @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("result", audit.search(q, page, 25));
        model.addAttribute("q", q == null ? "" : q);
        return "admin/audit";
    }

    // ------------------------------------------------------------ staff

    @GetMapping("/staff")
    public String staff(Model model) {
        model.addAttribute("staffList", staff.all());
        model.addAttribute("form", Map.of());
        return "admin/staff";
    }

    @PostMapping("/staff")
    public String createStaff(@RequestParam(required = false) String fullName, @RequestParam(required = false) String username,
                              @RequestParam(required = false) String role, @RequestParam(required = false) String password,
                              @RequestParam(required = false) String confirmPassword, Model model, HttpServletResponse res,
                              RedirectAttributes flash) {
        try {
            staff.create(username, fullName, password, confirmPassword, role, Web.currentUsername());
            flash.addFlashAttribute("flash", Flash.success("The account " + username + " was created."));
            return "redirect:/admin/staff";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("staffList", staff.all());
            model.addAttribute("form", Map.of("fullName", nz(fullName), "username", nz(username), "role", nz(role)));
            res.setStatus(422);
            return "admin/staff";
        }
    }

    @PostMapping("/staff/toggle")
    public String toggle(@RequestParam Long id, @RequestParam boolean active, RedirectAttributes flash) {
        try {
            staff.setActive(id, active, Web.currentUsername());
            flash.addFlashAttribute("flash", Flash.success("The account was updated."));
        } catch (ValidationException e) {
            flash.addFlashAttribute("flash", Flash.error(e.getMessage()));
        }
        return "redirect:/admin/staff";
    }

    @GetMapping("/password")
    public String password() {
        return "admin/password";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam(required = false) String currentPassword, @RequestParam(required = false) String newPassword,
                                 @RequestParam(required = false) String confirmPassword, Model model, HttpServletResponse res,
                                 RedirectAttributes flash) {
        try {
            staff.changePassword(Web.currentUsername(), currentPassword, newPassword, confirmPassword);
            flash.addFlashAttribute("flash", Flash.success("Your password was changed."));
            return Web.currentUser().isAdmin() ? "redirect:/admin" : "redirect:/scan";
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            res.setStatus(422);
            return "admin/password";
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
