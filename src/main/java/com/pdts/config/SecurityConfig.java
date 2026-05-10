package com.pdts.config;

import com.pdts.model.User;
import com.pdts.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepo;
    private final PdtsProperties pdtsProperties;

    public SecurityConfig(UserRepository userRepo, PdtsProperties pdtsProperties) {
        this.userRepo = userRepo;
        this.pdtsProperties = pdtsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF is disabled for this school/demo system so normal HTML forms work simply.
                // For real production use, enable CSRF again and include CSRF tokens in forms.
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Static files
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Public pages and portal pages
                        .requestMatchers(
                                "/",
                                "/login",
                                "/login-error",
                                "/error",
                                "/portal/**",
                                "/api/portal/**"
                        ).permitAll()

                        // Everything else requires login
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            String loginUsername = username == null ? "" : username.trim();

            User user = userRepo.findByUsername(loginUsername)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginUsername));

            if (user.getIsActive() == null || user.getIsActive() == 0) {
                throw new DisabledException("Account is deactivated.");
            }

            String roleName = user.getRole() != null ? user.getRole().getRoleName() : "USER";
            String authority = "ROLE_" + roleName.toUpperCase().replace(" ", "_");

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPasswordHash(),
                    List.of(new SimpleGrantedAuthority(authority))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        int strength = pdtsProperties.getBcryptStrength() > 0
                ? pdtsProperties.getBcryptStrength()
                : 10;

        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(strength);

        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword == null ? "" : rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                String typed = rawPassword == null ? "" : rawPassword.toString().trim();
                String saved = encodedPassword == null ? "" : encodedPassword.trim();

                // Supports demo seed passwords like {noop}Admin@2025
                if (saved.startsWith("{noop}")) {
                    return typed.equals(saved.substring(6));
                }

                // Supports BCrypt passwords
                if (saved.startsWith("$2a$") || saved.startsWith("$2b$") || saved.startsWith("$2y$")) {
                    return bcrypt.matches(typed, saved);
                }

                // Compatibility for older local demo records saved as plain text
                return typed.equals(saved);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
