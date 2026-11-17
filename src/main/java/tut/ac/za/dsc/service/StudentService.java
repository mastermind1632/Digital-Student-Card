package tut.ac.za.dsc.service;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tut.ac.za.dsc.config.AppProperties;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.domain.StudentPhoto;
import tut.ac.za.dsc.repository.StudentPhotoRepository;
import tut.ac.za.dsc.repository.StudentRepository;

/** Registration and changes to a student's details, with every check in one place. */
@Service
public class StudentService {

    private final StudentRepository students;
    private final StudentPhotoRepository photos;
    private final StudentNumberGenerator numbers;
    private final CardService cards;
    private final AuditService audit;
    private final PasswordEncoder passwords;
    private final AppProperties props;

    public StudentService(StudentRepository students, StudentPhotoRepository photos, StudentNumberGenerator numbers,
                          CardService cards, AuditService audit, PasswordEncoder passwords, AppProperties props) {
        this.students = students;
        this.photos = photos;
        this.numbers = numbers;
        this.cards = cards;
        this.audit = audit;
        this.passwords = passwords;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Optional<Student> find(String studentNumber) {
        return studentNumber == null ? Optional.empty() : students.findByStudentNumber(studentNumber.trim());
    }

    @Transactional(readOnly = true)
    public Student require(String studentNumber) {
        return find(studentNumber).orElseThrow(() -> new NotFoundException("No student has the number " + studentNumber + "."));
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> photo(String studentNumber) {
        return find(studentNumber).flatMap(s -> photos.findById(s.getId())).map(StudentPhoto::getContent);
    }

    /**
     * Registers a student and issues the card.
     *
     * @param byStaff true when an administrator registers the student (the card is valid at once)
     */
    @Transactional
    public Student register(RegistrationForm f, byte[] photoUpload, boolean byStaff, String actor) {
        Map<String, String> errors = new LinkedHashMap<>();
        SAIdNumber id = SAIdNumber.parse(f.getIdNumber());
        String name = Validators.clean(f.getFullName());
        String email = Validators.clean(f.getEmail());
        String phone = Validators.clean(f.getPhoneNumber());

        put(errors, "fullName", Validators.nameProblem(name));
        if (!id.isValid()) {
            errors.put("idNumber", id.getError());
        } else if (id.getAge() < 15) {
            errors.put("idNumber", "Students must be at least 15 years old.");
        } else if (students.findByIdNumber(id.getDigits()).isPresent()) {
            errors.put("idNumber", "A student with this ID number is already registered. Sign in instead, or contact the student card office.");
        }
        put(errors, "email", Validators.emailProblem(email));
        if (email != null && !errors.containsKey("email") && students.findByEmailIgnoreCase(email).isPresent()) {
            errors.put("email", "This email address is already used by another student.");
        }
        put(errors, "phoneNumber", Validators.phoneProblem(phone, true));
        if (Validators.clean(f.getCampus()) == null) errors.put("campus", "Choose your campus.");
        if (Validators.clean(f.getDepartment()) == null) errors.put("department", "Choose your faculty.");
        if (Validators.clean(f.getQualification()) == null) errors.put("qualification", "Enter the qualification you are registered for.");
        if (f.getYearOfStudy() == null || f.getYearOfStudy() < 1 || f.getYearOfStudy() > 7) errors.put("yearOfStudy", "Choose your year of study.");
        put(errors, "password", Validators.passwordProblem(f.getPassword()));
        if (f.getPassword() != null && !f.getPassword().equals(f.getConfirmPassword())) errors.put("confirmPassword", "The passwords do not match.");

        byte[] photo = null;
        if (photoUpload == null || photoUpload.length == 0) {
            errors.put("photo", "Add a clear photo of your face. It is printed on your card.");
        } else {
            try {
                photo = PhotoProcessor.process(photoUpload);
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        Student s = new Student();
        s.setFullName(name);
        s.setIdNumber(id.getDigits());
        s.setBirthDate(id.getBirthDate());
        s.setGender(id.getGender());
        s.setNationality(id.getCitizenship());
        s.setEmail(email.toLowerCase());
        s.setPhoneNumber(Validators.normalisePhone(phone));
        s.setCampus(Validators.clean(f.getCampus()));
        s.setDepartment(Validators.clean(f.getDepartment()));
        s.setQualification(Validators.clean(f.getQualification()));
        s.setYearOfStudy(f.getYearOfStudy());
        s.setPasswordHash(passwords.encode(f.getPassword()));
        s.setStudentNumber(numbers.next());
        s.setHasPhoto(true);
        cards.prepareNewCard(s, !byStaff && props.isRegistrationApproval());
        students.saveAndFlush(s);
        photos.save(new StudentPhoto(s.getId(), photo, "image/jpeg"));
        audit.log(actor == null ? s.getStudentNumber() : actor,
                byStaff ? "Student registered by staff" : "Student registered", s.getStudentNumber(), s.getFullName());
        return s;
    }

    @Transactional
    public void updateContactDetails(String studentNumber, String email, String phone) {
        Student s = require(studentNumber);
        Map<String, String> errors = new LinkedHashMap<>();
        String e = Validators.clean(email);
        String p = Validators.clean(phone);
        put(errors, "email", Validators.emailProblem(e));
        if (e != null && !errors.containsKey("email")) {
            students.findByEmailIgnoreCase(e).filter(o -> !o.getId().equals(s.getId()))
                    .ifPresent(o -> errors.put("email", "This email address is already used by another student."));
        }
        put(errors, "phoneNumber", Validators.phoneProblem(p, true));
        if (!errors.isEmpty()) throw new ValidationException(errors);
        s.setEmail(e.toLowerCase());
        s.setPhoneNumber(Validators.normalisePhone(p));
        audit.log(s.getStudentNumber(), "Contact details updated", s.getStudentNumber(), null);
    }

    @Transactional
    public void changePhoto(String studentNumber, byte[] upload, String actor) {
        Student s = require(studentNumber);
        byte[] jpeg = PhotoProcessor.process(upload);
        StudentPhoto existing = photos.findById(s.getId()).orElse(null);
        if (existing == null) {
            photos.save(new StudentPhoto(s.getId(), jpeg, "image/jpeg"));
        } else {
            existing.replace(jpeg, "image/jpeg");
        }
        s.setHasPhoto(true);
        audit.log(actor, "Photo changed", s.getStudentNumber(), null);
    }

    @Transactional
    public void changePassword(String studentNumber, String current, String next, String confirm) {
        Student s = require(studentNumber);
        if (current == null || !passwords.matches(current, s.getPasswordHash())) {
            throw ValidationException.of("currentPassword", "Your current password is not correct.");
        }
        String weak = Validators.passwordProblem(next);
        if (weak != null) throw ValidationException.of("newPassword", weak);
        if (!next.equals(confirm)) throw ValidationException.of("confirmPassword", "The passwords do not match.");
        if (next.equals(current)) throw ValidationException.of("newPassword", "Choose a password you have not used here before.");
        s.setPasswordHash(passwords.encode(next));
        audit.log(s.getStudentNumber(), "Password changed", s.getStudentNumber(), null);
    }

    /** Changes made by an administrator. The ID number cannot change; register a new student instead. */
    @Transactional
    public void adminUpdate(String studentNumber, RegistrationForm f, String actor) {
        Student s = require(studentNumber);
        Map<String, String> errors = new LinkedHashMap<>();
        String name = Validators.clean(f.getFullName());
        String e = Validators.clean(f.getEmail());
        String p = Validators.clean(f.getPhoneNumber());
        put(errors, "fullName", Validators.nameProblem(name));
        put(errors, "email", Validators.emailProblem(e));
        if (e != null && !errors.containsKey("email")) {
            students.findByEmailIgnoreCase(e).filter(o -> !o.getId().equals(s.getId()))
                    .ifPresent(o -> errors.put("email", "This email address is already used by another student."));
        }
        put(errors, "phoneNumber", Validators.phoneProblem(p, false));
        if (Validators.clean(f.getCampus()) == null) errors.put("campus", "Choose a campus.");
        if (Validators.clean(f.getDepartment()) == null) errors.put("department", "Choose a faculty.");
        if (f.getYearOfStudy() != null && (f.getYearOfStudy() < 1 || f.getYearOfStudy() > 7)) errors.put("yearOfStudy", "Choose a year between 1 and 7.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        StringBuilder changes = new StringBuilder();
        if (!name.equals(s.getFullName())) changes.append("name; ");
        if (!e.equalsIgnoreCase(s.getEmail())) changes.append("email; ");
        if (!String.valueOf(Validators.normalisePhone(p)).equals(String.valueOf(s.getPhoneNumber()))) changes.append("phone; ");
        if (!f.getCampus().equals(s.getCampus())) changes.append("campus; ");
        if (!f.getDepartment().equals(s.getDepartment())) changes.append("faculty; ");
        s.setFullName(name);
        s.setEmail(e.toLowerCase());
        s.setPhoneNumber(Validators.normalisePhone(p));
        s.setCampus(Validators.clean(f.getCampus()));
        s.setDepartment(Validators.clean(f.getDepartment()));
        s.setQualification(Validators.clean(f.getQualification()));
        s.setYearOfStudy(f.getYearOfStudy());
        audit.log(actor, "Student details edited", s.getStudentNumber(), changes.length() == 0 ? "No changes" : changes.toString());
    }

    /** Sets a random temporary password and returns it so staff can give it to the student. */
    @Transactional
    public String resetPassword(String studentNumber, String actor) {
        Student s = require(studentNumber);
        String temp = temporaryPassword();
        s.setPasswordHash(passwords.encode(temp));
        audit.log(actor, "Password reset", s.getStudentNumber(), null);
        return temp;
    }

    @Transactional
    public void recordLogin(String studentNumber) {
        find(studentNumber).ifPresent(s -> s.setLastLoginAt(java.time.LocalDateTime.now()));
    }

    @Transactional
    public void delete(String studentNumber, String confirm, String actor) {
        Student s = require(studentNumber);
        if (!studentNumber.equals(confirm == null ? null : confirm.replaceAll("\\s", ""))) {
            throw ValidationException.of("confirm", "Type the student number to confirm the deletion.");
        }
        photos.findById(s.getId()).ifPresent(photos::delete);
        students.delete(s);
        audit.log(actor, "Student deleted", studentNumber, s.getFullName());
    }

    // ------------------------------------------------------------

    private static void put(Map<String, String> errors, String field, String problem) {
        if (problem != null) errors.put(field, problem);
    }

    static String temporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ", lower = "abcdefghijkmnpqrstuvwxyz", digits = "23456789", symbols = "!@#$%";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(r.nextInt(upper.length())));
        for (int i = 0; i < 5; i++) sb.append(lower.charAt(r.nextInt(lower.length())));
        sb.append(symbols.charAt(r.nextInt(symbols.length())));
        for (int i = 0; i < 3; i++) sb.append(digits.charAt(r.nextInt(digits.length())));
        return sb.toString();
    }
}
