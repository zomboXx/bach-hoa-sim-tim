package vn.simtim.api.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.simtim.api.auth.application.AuthService;

@Configuration
public class AuthSecurity {
    @Bean Clock authClock() { return Clock.systemUTC(); }

    @Bean
    SecurityFilterChain security(HttpSecurity http, AuthService service, ObjectMapper mapper, LoginRateLimiter limiter) throws Exception {
        var bearer = new BearerSessionFilter(service, mapper, limiter);
        String[] catalog = {"/api/v1/categories", "/api/v1/categories/**", "/api/v1/units", "/api/v1/units/**",
                "/api/v1/products", "/api/v1/products/**", "/api/v1/suppliers", "/api/v1/suppliers/**"};
        String[] invoices = {"/api/v1/invoices", "/api/v1/invoices/**"};
        return http
                // Credentials are only accepted in an explicit Authorization header, never cookies or Basic auth.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/session").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                    .requestMatchers(HttpMethod.GET, catalog).hasAuthority("catalog.read")
                    .requestMatchers(HttpMethod.POST, catalog).hasAuthority("catalog.write")
                    .requestMatchers(HttpMethod.PUT, catalog).hasAuthority("catalog.write")
                    .requestMatchers(HttpMethod.DELETE, catalog).hasAuthority("catalog.write")
                    .requestMatchers(HttpMethod.GET, invoices).hasAuthority("invoices.read")
                    .requestMatchers(HttpMethod.POST, invoices).hasAuthority("invoices.write")
                    .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request, response, ex) -> bearer.writeError(response, 401, "UNAUTHENTICATED", "Valid bearer session required"))
                    .accessDeniedHandler((request, response, ex) -> bearer.writeError(response, 403, "FORBIDDEN", "Permission denied")))
                .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class).build();
    }
}
