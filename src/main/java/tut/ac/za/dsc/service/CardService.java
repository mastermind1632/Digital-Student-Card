package tut.ac.za.dsc.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.AppSetting;
import tut.ac.za.dsc.domain.CardStatus;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.domain.VerificationLog;
import tut.ac.za.dsc.domain.VerificationResult;
import tut.ac.za.dsc.repository.AppSettingRepository;
import tut.ac.za.dsc.repository.StudentRepository;
import tut.ac.za.dsc.repository.VerificationLogRepository;

/**
 * Card lifecycle (approve, renew, suspend, replace) and verification.
 *
 * The QR code on a card holds a link like {@code /verify?sn=261234567&v=2&t=Xy...}. The token t is an HMAC
 * signature of the student number and card version, made with a secret stored in the database. A QR code
 * therefore cannot be forged by typing someone else's number, and codes on replaced cards stop working.
 */
@Service
public class CardService {

    private static final String SECRET_KEY = "qr.signing.key";

    private final StudentRepository students;
    private final AppSettingRepository settings;
    private final VerificationLogRepository checks;
    private final AuditService audit;
    private final AppProperties props;
    private volatile byte[] secret;

    public CardService(StudentRepository students, AppSettingRepository settings, VerificationLogRepository checks,
                       AuditService audit, AppProperties props) {
        this.students = students;
        this.settings = settings;
        this.checks = checks;
        this.audit = audit;
        this.props = props;
    }

    // ------------------------------------------------------------ lifecycle

    void prepareNewCard(Student s, boolean needsApproval) {
        s.setCardVersion(1);
        s.setIssueDate(LocalDate.now());
        s.setExpiryDate(LocalDate.now().plusMonths(props.getCardValidityMonths()));
        s.setStatus(needsApproval ? CardStatus.PENDING : CardStatus.ACTIVE);
    }

    @Transactional
    public Student approve(String studentNumber, String actor) {
        Student s = require(studentNumber);
        s.setStatus(CardStatus.ACTIVE);
        s.setStatusReason(null);
        s.setIssueDate(LocalDate.now());
        s.setExpiryDate(LocalDate.now().plusMonths(props.getCardValidityMonths()));
        audit.log(actor, "Card approved", s.getStudentNumber(), s.getFullName());
        return s;
    }

    @Transactional
    public Student suspend(String studentNumber, String reason, String actor) {
        Student s = require(studentNumber);
        String why = Validators.clean(reason);
        if (why == null) throw ValidationException.of("reason", "Say why the card is being suspended. The student will see this reason.");
        s.setStatus(CardStatus.SUSPENDED);
        s.setStatusReason(why.length() > 250 ? why.substring(0, 250) : why);
        audit.log(actor, "Card suspended", s.getStudentNumber(), why);
        return s;
    }

    @Transactional
    public Student revoke(String studentNumber, String reason, String actor) {
        Student s = require(studentNumber);
        String why = Validators.clean(reason);
        if (why == null) throw ValidationException.of("reason", "Say why the card is being revoked.");
        s.setStatus(CardStatus.REVOKED);
        s.setStatusReason(why.length() > 250 ? why.substring(0, 250) : why);
        audit.log(actor, "Card revoked", s.getStudentNumber(), why);
        return s;
    }

    @Transactional
    public Student reactivate(String studentNumber, String actor) {
        Student s = require(studentNumber);
        s.setStatus(CardStatus.ACTIVE);
        s.setStatusReason(null);
        audit.log(actor, "Card reactivated", s.getStudentNumber(), null);
        return s;
    }

    /** Extends the card from today, or from its current expiry date if that is later. */
    @Transactional
    public Student renew(String studentNumber, String actor) {
        Student s = require(studentNumber);
        LocalDate base = s.getExpiryDate() != null && s.getExpiryDate().isAfter(LocalDate.now()) ? s.getExpiryDate() : LocalDate.now();
        s.setExpiryDate(base.plusMonths(props.getCardValidityMonths()));
        audit.log(actor, "Card renewed", s.getStudentNumber(), "Valid until " + s.getExpiryDateText());
        return s;
    }

    /** Replacement card for a lost phone or printed card: the QR code on the old card stops working at once. */
    @Transactional
    public Student reissue(String studentNumber, String actor) {
        Student s = require(studentNumber);
        s.setCardVersion(s.getCardVersion() + 1);
        s.setIssueDate(LocalDate.now());
        audit.log(actor, "Replacement card issued", s.getStudentNumber(), "Card version " + s.getCardVersion());
        return s;
    }

    // ------------------------------------------------------------ signed QR link

    /** Path and query string (without the host) that the card's QR code points to. */
    public String verificationPath(Student s) {
        return "/verify?sn=" + s.getStudentNumber() + "&v=" + s.getCardVersion() + "&t=" + sign(s.getStudentNumber(), s.getCardVersion());
    }

    /** True when t is a genuine signature for this student number and card version (current or old). */
    public boolean tokenMatches(String studentNumber, Integer version, String token) {
        if (studentNumber == null || token == null || !studentNumber.matches("\\d{6,12}")) return false;
        return constantTimeEquals(sign(studentNumber, version == null ? 1 : version), token);
    }

    /**
     * Checks a card and records the check.
     *
     * @param token  t from the QR link, or null when a number was typed in (or an old QR held only the number)
     * @param method SCANNER, PUBLIC_LINK or MANUAL
     */
    @Transactional
    public VerificationOutcome verify(String studentNumber, Integer version, String token, String method,
                                      String checkedBy, String location, String ip) {
        String sn = studentNumber == null ? "" : studentNumber.replaceAll("\\s", "");
        Student s = sn.matches("\\d{6,12}") ? students.findByStudentNumber(sn).orElse(null) : null;
        VerificationResult result;
        if (s == null) {
            result = VerificationResult.NOT_FOUND;
        } else if (token != null) {
            int v = version == null ? 1 : version;
            if (!tokenMatches(sn, v, token)) {
                result = VerificationResult.TAMPERED;
            } else if (v != s.getCardVersion()) {
                result = VerificationResult.SUPERSEDED;
            } else {
                result = VerificationResult.of(s.getEffectiveStatus());
            }
        } else {
            result = VerificationResult.of(s.getEffectiveStatus());
        }

        VerificationLog log = new VerificationLog();
        log.setStudentNumber(sn.length() > 20 ? sn.substring(0, 20) : sn);
        log.setStudentName(s == null ? null : s.getFullName());
        log.setResult(result);
        log.setMethod(method);
        log.setCheckedBy(checkedBy);
        String where = Validators.clean(location);
        log.setLocation(where != null && where.length() > 80 ? where.substring(0, 80) : where);
        log.setIpAddress(ip);
        checks.save(log);
        return new VerificationOutcome(result, result == VerificationResult.TAMPERED ? null : s);
    }

    String sign(String studentNumber, int version) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret(), "HmacSHA256"));
            byte[] sig = mac.doFinal((studentNumber + ":" + version).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig).substring(0, 22);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign the card", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }

    /** The signing key: created once, stored in app_setting so QR codes keep working after restarts. */
    private byte[] secret() {
        byte[] s = secret;
        if (s != null) return s;
        synchronized (this) {
            if (secret == null) {
                AppSetting setting = settings.findById(SECRET_KEY).orElseGet(() -> {
                    byte[] fresh = new byte[32];
                    new SecureRandom().nextBytes(fresh);
                    return settings.saveAndFlush(new AppSetting(SECRET_KEY, Base64.getEncoder().encodeToString(fresh)));
                });
                secret = Base64.getDecoder().decode(setting.getValue());
            }
            return secret;
        }
    }

    /** Makes sure the signing key exists (called once at start-up). */
    public void ensureSigningKey() {
        secret();
    }

    /** Used by tests to sign without a database. */
    void useSecretForTests(byte[] key) {
        this.secret = key;
    }

    private Student require(String studentNumber) {
        return students.findByStudentNumber(studentNumber)
                .orElseThrow(() -> ValidationException.of("studentNumber", "No student has the number " + studentNumber + "."));
    }
}
