package tut.ac.za.dsc.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The signed-in person: a student (username = student number) or a staff member (username = staff username). */
public class AppUser implements UserDetails {

    public enum Kind { STUDENT, ADMIN, SECURITY }

    private final Kind kind;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final boolean enabled;

    public AppUser(Kind kind, String username, String displayName, String passwordHash, boolean enabled) {
        this.kind = kind;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    /** Copy without the password hash, kept in the session after sign-in. */
    public AppUser withoutPassword() {
        return new AppUser(kind, username, displayName, null, enabled);
    }

    public Kind getKind() { return kind; }
    public String getDisplayName() { return displayName; }
    public boolean isStudent() { return kind == Kind.STUDENT; }
    public boolean isStaff() { return kind != Kind.STUDENT; }
    public boolean isAdmin() { return kind == Kind.ADMIN; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + kind.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return enabled; }
}
