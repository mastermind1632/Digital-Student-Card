package tut.ac.za.dsc.startup;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.service.CardService;
import tut.ac.za.dsc.service.RegistrationForm;
import tut.ac.za.dsc.service.StaffService;
import tut.ac.za.dsc.service.StudentService;
import tut.ac.za.dsc.service.ValidationException;

/**
 * Runs once the application has started: creates the QR signing key and, when {@code app.demo-seed=true}
 * and the database is still empty, adds demo staff and students so the app can be tried straight away.
 */
@Component
public class StartupTasks {

    private static final Logger log = LoggerFactory.getLogger(StartupTasks.class);

    public static final String DEMO_STUDENT_PASSWORD = "Student#2026";

    private final CardService cards;
    private final StaffService staff;
    private final StudentService students;
    private final AppProperties props;

    public StartupTasks(CardService cards, StaffService staff, StudentService students, AppProperties props) {
        this.cards = cards;
        this.staff = staff;
        this.students = students;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        cards.ensureSigningKey();
        if (props.isDemoSeed() && staff.noStaffYet()) {
            try {
                seed();
                log.info("Demo data added. Staff: admin / Admin#2026 and security.gate1 / Gate#2026x. "
                        + "Students sign in with their email and {}", DEMO_STUDENT_PASSWORD);
            } catch (RuntimeException e) {
                log.warn("Demo data could not be added: {}", e.getMessage());
            }
        }
    }

    private void seed() {
        staff.create("admin", "Card Office Administrator", "Admin#2026", "Admin#2026", "ADMIN", "demo");
        staff.create("security.gate1", "Main Gate Security", "Gate#2026x", "Gate#2026x", "SECURITY", "demo");

        String campus0 = pick(props.getCampuses(), 0, "Pretoria Campus");
        String campus1 = pick(props.getCampuses(), 1, campus0);
        String campus2 = pick(props.getCampuses(), 2, campus0);
        String dep0 = pick(props.getDepartments(), 0, "Faculty of Information and Communication Technology");
        String dep1 = pick(props.getDepartments(), 1, dep0);
        String dep2 = pick(props.getDepartments(), 2, dep0);

        // Fictional people; the ID numbers have valid dates and check digits but belong to nobody.
        add("Thabo Mokoena", "0304155123082", "thabo.mokoena@example.com", "0821234567", campus0, dep0, "Diploma in Information Technology", 3, true);
        add("Lerato Dlamini", "0408220187087", "lerato.dlamini@example.com", "0739876543", campus1, dep1, "Bachelor of Engineering Technology", 2, true);
        add("Sipho Nkosi", "0203115741084", "sipho.nkosi@example.com", "0614567890", campus0, dep0, "Advanced Diploma in Computer Science", 4, true);
        add("Naledi Khumalo", "0511301125080", "naledi.khumalo@example.com", "0712223344", campus2, dep2, "Higher Certificate in Business", 1, false);
        add("Ayanda Zulu", "0106050933089", "ayanda.zulu@example.com", "0835556677", campus1, dep1, "Diploma in Civil Engineering", 3, false);
        add("Johan van der Merwe", "8501015800088", "johan.vdmerwe@example.com", "0798887766", campus0, dep2, "Postgraduate Diploma in Management", 1, true);
    }

    private void add(String name, String id, String email, String phone, String campus, String dep, String qual,
                     int year, boolean byStaff) {
        RegistrationForm f = new RegistrationForm();
        f.setFullName(name);
        f.setIdNumber(id);
        f.setEmail(email);
        f.setPhoneNumber(phone);
        f.setCampus(campus);
        f.setDepartment(dep);
        f.setQualification(qual);
        f.setYearOfStudy(year);
        f.setPassword(DEMO_STUDENT_PASSWORD);
        f.setConfirmPassword(DEMO_STUDENT_PASSWORD);
        try {
            students.register(f, avatar(name), byStaff, "demo");
        } catch (ValidationException e) {
            log.warn("Demo student {} skipped: {}", name, e.getErrors());
        } catch (RuntimeException e) {
            log.warn("Demo student {} skipped: {}", name, e.toString());
        }
    }

    private static String pick(java.util.List<String> list, int i, String fallback) {
        return list != null && list.size() > i ? list.get(i) : fallback;
    }

    /** A simple illustrated portrait (no real person) so demo cards have a photo. Draws shapes only, no text, so it needs no fonts. */
    static byte[] avatar(String name) {
        int w = 480, h = 640;
        int hash = Math.abs(name.hashCode());
        Color[] backs = {new Color(0x1F4E79), new Color(0x0E6B5C), new Color(0x6B2D5C), new Color(0x7A4B12), new Color(0x2E3A87)};
        Color[] skins = {new Color(0x8D5524), new Color(0xC68642), new Color(0x6B3E26), new Color(0xE0AC69), new Color(0x5C3317)};
        Color back = backs[hash % backs.length];
        Color skin = skins[(hash / 7) % skins.length];
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, back.brighter(), 0, h, back.darker()));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x2B2B2B));
        g.fill(new Ellipse2D.Double(60, 470, 360, 340));           // shoulders
        g.setColor(skin.darker());
        g.fillRect(200, 380, 80, 110);                              // neck
        g.setColor(new Color(0x1A1A1A));
        g.fill(new Ellipse2D.Double(125, 105, 230, 210));          // hair
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(140, 150, 200, 260));          // face
        g.setColor(new Color(0x1A1A1A));
        g.fill(new Ellipse2D.Double(190, 255, 22, 14));
        g.fill(new Ellipse2D.Double(268, 255, 22, 14));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(skin.darker().darker());
        g.drawArc(205, 320, 70, 35, 200, 140);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
