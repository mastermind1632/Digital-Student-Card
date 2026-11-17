package tut.ac.za.dsc.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/** Normal username/password check, refused while the account or address is locked by {@link LoginAttempts}. */
public class ThrottledAuthenticationProvider implements AuthenticationProvider {

    private final DaoAuthenticationProvider delegate;
    private final LoginAttempts attempts;

    public ThrottledAuthenticationProvider(UserDetailsService accounts, PasswordEncoder encoder, LoginAttempts attempts) {
        this.delegate = new DaoAuthenticationProvider(accounts);
        this.delegate.setPasswordEncoder(encoder);
        this.attempts = attempts;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        String account = String.valueOf(auth.getName()).trim().toLowerCase();
        String address = auth.getDetails() instanceof WebAuthenticationDetails d ? d.getRemoteAddress() : "unknown";
        long locked = attempts.minutesLocked(account, address);
        if (locked > 0) {
            throw new LockedException("Too many failed attempts. Try again in " + locked + " minute" + (locked == 1 ? "" : "s") + ".");
        }
        try {
            Authentication result = delegate.authenticate(auth);
            attempts.succeeded(account, address);
            AppUser user = (AppUser) result.getPrincipal();
            return UsernamePasswordAuthenticationToken.authenticated(user.withoutPassword(), null, user.getAuthorities());
        } catch (BadCredentialsException e) {
            attempts.failed(account, address);
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}
