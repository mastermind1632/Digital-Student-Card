package tut.ac.za.dsc.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import tut.ac.za.dsc.domain.StaffUser;
import tut.ac.za.dsc.domain.Student;
import tut.ac.za.dsc.repository.StaffUserRepository;
import tut.ac.za.dsc.repository.StudentRepository;

/** Finds the account for each sign-in form. Students sign in with their student number or email address. */
@Component
public class AccountLookup {

    private final StudentRepository students;
    private final StaffUserRepository staff;

    public AccountLookup(StudentRepository students, StaffUserRepository staff) {
        this.students = students;
        this.staff = staff;
    }

    public UserDetailsService studentAccounts() {
        return new UserDetailsService() {
            @Override
            public AppUser loadUserByUsername(String login) {
                String key = login == null ? "" : login.trim();
                Student s = (key.contains("@") ? students.findByEmailIgnoreCase(key) : students.findByStudentNumber(key.replaceAll("\\s", "")))
                        .orElseThrow(() -> new UsernameNotFoundException("No such student"));
                return new AppUser(AppUser.Kind.STUDENT, s.getStudentNumber(), s.getFullName(), s.getPasswordHash(), true);
            }
        };
    }

    public UserDetailsService staffAccounts() {
        return new UserDetailsService() {
            @Override
            public AppUser loadUserByUsername(String username) {
                StaffUser u = staff.findByUsernameIgnoreCase(username == null ? "" : username.trim())
                        .orElseThrow(() -> new UsernameNotFoundException("No such staff member"));
                AppUser.Kind kind = u.isAdmin() ? AppUser.Kind.ADMIN : AppUser.Kind.SECURITY;
                return new AppUser(kind, u.getUsername(), u.getFullName(), u.getPasswordHash(), u.isActive());
            }
        };
    }
}
