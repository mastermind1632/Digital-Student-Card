package tut.ac.za.dsc.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.Student;

/** Emails a student's card as a PDF attachment. Only active when SPRING_MAIL_HOST is configured. */
@Service
public class MailService {

    private final ObjectProvider<JavaMailSender> sender;
    private final AppProperties props;
    private final String host;

    public MailService(ObjectProvider<JavaMailSender> sender, AppProperties props, @Value("${spring.mail.host:}") String host) {
        this.sender = sender;
        this.props = props;
        this.host = host;
    }

    /** True when an SMTP server is configured (docker-compose passes an empty SPRING_MAIL_HOST when it is not). */
    public boolean isEnabled() {
        return host != null && !host.isBlank() && sender.getIfAvailable() != null;
    }

    /** Sends the card. Returns null on success, or a sentence explaining what went wrong. */
    public String sendCard(Student s, byte[] pdf, String verifyUrl) {
        JavaMailSender mail = isEnabled() ? sender.getIfAvailable() : null;
        if (mail == null) return "Email is not set up on this server yet. Download the PDF instead.";
        try {
            MimeMessage msg = mail.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(props.getMailFrom());
            h.setTo(s.getEmail());
            h.setSubject("Your student card - " + props.getInstitutionName());
            h.setText(text(s, verifyUrl), html(s, verifyUrl));
            h.addAttachment("student-card-" + s.getStudentNumber() + ".pdf", new ByteArrayResource(pdf), "application/pdf");
            mail.send(msg);
            return null;
        } catch (MailAuthenticationException e) {
            return "The email account rejected the sign-in. For Gmail, use a 16-character app password, not your normal password.";
        } catch (MailException | jakarta.mail.MessagingException e) {
            return "The email could not be sent: " + e.getMessage();
        }
    }

    private String text(Student s, String url) {
        return "Hello " + s.getFirstName() + ",\n\n"
                + "Your student card is attached as a PDF. You can print it or keep it on your phone.\n\n"
                + "Student number: " + s.getStudentNumber() + "\n"
                + "Qualification: " + nz(s.getQualification()) + "\n"
                + "Campus: " + nz(s.getCampus()) + "\n"
                + "Valid until: " + s.getExpiryDateText() + "\n\n"
                + "Anyone can check that your card is genuine by scanning its QR code, or at:\n" + url + "\n\n"
                + "If you lose your phone or printed card, contact " + props.getCardOffice() + " so they can issue a replacement.\n\n"
                + props.getInstitutionName();
    }

    private String html(Student s, String url) {
        return "<!doctype html><html><body style=\"margin:0;background:#f5f5f9;font-family:Arial,Helvetica,sans-serif;color:#16182b\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:32px 16px\">"
                + "<table role=\"presentation\" width=\"520\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:520px;background:#ffffff;border:1px solid #e3e2ec;border-radius:12px\">"
                + "<tr><td style=\"background:#3b2a8c;color:#ffffff;padding:20px 28px;border-radius:12px 12px 0 0;font-size:14px\">" + esc(props.getInstitutionName()) + "</td></tr>"
                + "<tr><td style=\"padding:28px\"><h1 style=\"font-size:22px;margin:0 0 12px\">Your student card is ready</h1>"
                + "<p style=\"font-size:15px;line-height:1.55;margin:0 0 20px\">Hello " + esc(s.getFirstName())
                + ", your card is attached as a PDF. Print it, or keep it on your phone and show it from the student portal.</p>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:14px;line-height:1.8\">"
                + row("Student number", s.getStudentNumberDisplay()) + row("Qualification", s.getQualification())
                + row("Campus", s.getCampus()) + row("Valid until", s.getExpiryDateText()) + "</table>"
                + "<p style=\"margin:24px 0 0\"><a href=\"" + esc(url) + "\" style=\"background:#5b3fd6;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:8px;font-weight:bold;display:inline-block\">Check my card online</a></p>"
                + "<p style=\"font-size:13px;color:#62647a;line-height:1.5;margin:24px 0 0\">If you lose your phone or printed card, contact "
                + esc(props.getCardOffice()) + " so a replacement can be issued and the old card stops working.</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"color:#62647a;padding-right:24px\">" + esc(label) + "</td><td style=\"font-weight:bold\">" + esc(nz(value)) + "</td></tr>";
    }

    private static String nz(String s) {
        return s == null ? "-" : s;
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
