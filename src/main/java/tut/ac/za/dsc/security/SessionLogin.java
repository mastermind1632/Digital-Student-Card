package tut.ac.za.dsc.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Signs a person in straight after they create an account (registration, first administrator). */
@Component
public class SessionLogin {

    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public void signIn(AppUser user, HttpServletRequest req, HttpServletResponse res) {
        req.getSession(true);
        req.changeSessionId(); // a new session id on sign-in (protects against session fixation)
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.withoutPassword(), null, user.getAuthorities()));
        SecurityContextHolder.setContext(ctx);
        repository.saveContext(ctx, req, res);
    }
}
