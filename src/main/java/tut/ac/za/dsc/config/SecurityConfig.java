package tut.ac.za.dsc.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import tut.ac.za.dsc.security.AccountLookup;
import tut.ac.za.dsc.security.AppUser;
import tut.ac.za.dsc.security.LoginAttempts;
import tut.ac.za.dsc.security.ThrottledAuthenticationProvider;
import tut.ac.za.dsc.service.StaffService;
import tut.ac.za.dsc.service.StudentService;

/**
 * Two sign-in areas:
 *  - staff (/admin/**, /scan/**) with their own login page; security officers may only scan;
 *  - students (/student/**) with the student login page. Public pages: home, register, verify.
 */
@Configuration
public class SecurityConfig {

    static final String CSP = "default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; font-src 'self'; "
            + "script-src 'self'; connect-src 'self'; media-src 'self' blob:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'";

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // bcrypt
    }

    @Bean
    @Order(1)
    SecurityFilterChain staffChain(HttpSecurity http, AccountLookup accounts, PasswordEncoder encoder, LoginAttempts attempts,
                                   StaffService staffService) throws Exception {
        http.securityMatcher("/admin", "/admin/**", "/scan", "/scan/**")
            .authenticationManager(new ProviderManager(new ThrottledAuthenticationProvider(accounts.staffAccounts(), encoder, attempts)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/admin/login", "/admin/setup").permitAll()
                .requestMatchers("/scan", "/scan/**", "/admin/password").hasAnyRole("ADMIN", "SECURITY")
                .anyRequest().hasRole("ADMIN"))
            .formLogin(f -> f
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((req, res, auth) -> {
                    AppUser u = (AppUser) auth.getPrincipal();
                    staffService.recordLogin(u.getUsername());
                    redirectAfterLogin(req, res, auth, u.isAdmin() ? "/admin" : "/scan");
                })
                .failureHandler(failure("/admin/login")))
            .logout(l -> l.logoutUrl("/admin/logout").logoutSuccessUrl("/admin/login?signedOut"))
            .exceptionHandling(e -> e
                .accessDeniedPage("/denied")
                // the scanner calls /scan/check with fetch(): answer 401 instead of redirecting to the login page
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")));
        return headers(http).build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain studentChain(HttpSecurity http, AccountLookup accounts, PasswordEncoder encoder, LoginAttempts attempts,
                                     StudentService studentService) throws Exception {
        http.authenticationManager(new ProviderManager(new ThrottledAuthenticationProvider(accounts.studentAccounts(), encoder, attempts)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/student", "/student/**").hasRole("STUDENT")
                .requestMatchers("/media/qr").authenticated()
                .anyRequest().permitAll())
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("studentNumber")
                .passwordParameter("password")
                .successHandler((req, res, auth) -> {
                    AppUser u = (AppUser) auth.getPrincipal();
                    if (u.isStudent()) studentService.recordLogin(u.getUsername());
                    redirectAfterLogin(req, res, auth, "/student");
                })
                .failureHandler(failure("/login")))
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?signedOut"))
            .exceptionHandling(e -> e.accessDeniedPage("/denied"));
        return headers(http).build();
    }

    private static HttpSecurity headers(HttpSecurity http) throws Exception {
        return http.headers(h -> h
            .contentSecurityPolicy(c -> c.policyDirectives(CSP))
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
            .permissionsPolicyHeader(p -> p.policy("camera=(self), microphone=(), geolocation=()")));
    }

    private static void redirectAfterLogin(HttpServletRequest req, HttpServletResponse res, Authentication auth, String fallback)
            throws IOException, jakarta.servlet.ServletException {
        SavedRequestAwareAuthenticationSuccessHandler h = new SavedRequestAwareAuthenticationSuccessHandler();
        h.setDefaultTargetUrl(fallback);
        h.onAuthenticationSuccess(req, res, auth);
    }

    /** Sends the person back to the login page with a message saying what went wrong. */
    private static AuthenticationFailureHandler failure(String loginPage) {
        return (HttpServletRequest req, HttpServletResponse res, AuthenticationException e) -> {
            String reason = e instanceof LockedException ? "locked" : e instanceof DisabledException ? "disabled" : "bad";
            req.getSession().setAttribute("loginMessage", e instanceof LockedException ? e.getMessage() : null);
            res.sendRedirect(req.getContextPath() + loginPage + "?error=" + reason);
        };
    }
}
