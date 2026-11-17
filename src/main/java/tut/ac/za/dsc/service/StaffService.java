package tut.ac.za.dsc.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tut.ac.za.dsc.domain.StaffUser;
import tut.ac.za.dsc.repository.StaffUserRepository;

/** Administrator and security officer accounts. */
@Service
public class StaffService {

    private final StaffUserRepository staff;
    private final PasswordEncoder passwords;
    private final AuditService audit;

    public StaffService(StaffUserRepository staff, PasswordEncoder passwords, AuditService audit) {
        this.staff = staff;
        this.passwords = passwords;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public boolean noStaffYet() {
        return staff.count() == 0;
    }

    @Transactional(readOnly = true)
    public List<StaffUser> all() {
        return staff.findAllByOrderByUsernameAsc();
    }

    @Transactional
    public StaffUser create(String username, String fullName, String password, String confirm, String role, String actor) {
        Map<String, String> errors = new LinkedHashMap<>();
        String user = Validators.clean(username);
        if (user == null || !user.matches("[A-Za-z0-9._-]{3,40}")) {
            errors.put("username", "Use 3 to 40 letters, numbers, dots, dashes or underscores.");
        } else if (staff.findByUsernameIgnoreCase(user).isPresent()) {
            errors.put("username", "This username is already taken.");
        }
        String name = Validators.clean(fullName);
        if (name == null) errors.put("fullName", "Enter the person's name.");
        String weak = Validators.passwordProblem(password);
        if (weak != null) errors.put("password", weak);
        else if (!password.equals(confirm)) errors.put("confirmPassword", "The passwords do not match.");
        StaffUser.Role r = null;
        try {
            r = StaffUser.Role.valueOf(String.valueOf(role));
        } catch (IllegalArgumentException e) {
            errors.put("role", "Choose a role.");
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        StaffUser u = new StaffUser();
        u.setUsername(user.toLowerCase());
        u.setFullName(name);
        u.setPasswordHash(passwords.encode(password));
        u.setRole(r);
        u.setActive(true);
        staff.save(u);
        audit.log(actor, "Staff account created", u.getUsername(), u.getRoleLabel());
        return u;
    }

    @Transactional
    public void setActive(Long id, boolean active, String actor) {
        StaffUser u = staff.findById(id).orElseThrow(() -> new NotFoundException("No such staff account."));
        if (u.getUsername().equalsIgnoreCase(actor)) throw ValidationException.of("staff", "You cannot switch off your own account.");
        if (!active && u.isAdmin() && staff.countByRoleAndActiveTrue(StaffUser.Role.ADMIN) <= 1) {
            throw ValidationException.of("staff", "You cannot switch off the last active administrator.");
        }
        u.setActive(active);
        audit.log(actor, active ? "Staff account switched on" : "Staff account switched off", u.getUsername(), null);
    }

    @Transactional
    public void changePassword(String username, String current, String next, String confirm) {
        StaffUser u = staff.findByUsernameIgnoreCase(username).orElseThrow(() -> new NotFoundException("No such staff account."));
        if (current == null || !passwords.matches(current, u.getPasswordHash())) {
            throw ValidationException.of("currentPassword", "Your current password is not correct.");
        }
        String weak = Validators.passwordProblem(next);
        if (weak != null) throw ValidationException.of("newPassword", weak);
        if (!next.equals(confirm)) throw ValidationException.of("confirmPassword", "The passwords do not match.");
        u.setPasswordHash(passwords.encode(next));
        audit.log(u.getUsername(), "Password changed", u.getUsername(), null);
    }

    @Transactional
    public void recordLogin(String username) {
        staff.findByUsernameIgnoreCase(username).ifPresent(u -> u.setLastLoginAt(LocalDateTime.now()));
    }
}
