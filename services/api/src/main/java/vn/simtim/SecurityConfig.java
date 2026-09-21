package vn.simtim;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(AccountRepository repo) {
    return id -> {
      var a =
          repo.findById(id.toUpperCase())
              .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
      return User.withUsername(a.id).password(a.password).roles(a.role).disabled(!a.active).build();
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/health", "/api/csrf", "/api/auth/login")
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/products",
                        "/api/categories",
                        "/api/suppliers",
                        "/api/promotions",
                        "/api/counts/*/approve",
                        "/api/counts")
                    .hasAnyRole("admin", "manager")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.PUT,
                        "/api/products/**",
                        "/api/categories/**",
                        "/api/suppliers/**")
                    .hasAnyRole("admin", "manager")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.DELETE,
                        "/api/products/**",
                        "/api/categories/**",
                        "/api/suppliers/**")
                    .hasAnyRole("admin", "manager")
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/receipts")
                    .hasAnyRole("admin", "stock")
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/invoices")
                    .hasAnyRole("admin", "sales")
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/suppliers")
                    .hasAnyRole("admin", "manager", "stock")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/products", "/api/categories")
                    .hasAnyRole("admin", "manager", "sales", "stock")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/me", "/api/state")
                    .hasAnyRole("admin", "accountant", "manager", "sales", "stock")
                    .requestMatchers("/api/training/**")
                    .hasAnyRole("admin", "accountant", "manager", "sales", "stock")
                    .anyRequest()
                    .denyAll())
        .csrf(
            c ->
                c.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .successHandler(
                        (q, r, a) -> {
                          r.setContentType("application/json;charset=UTF-8");
                          r.getWriter().write("{\"ok\":true}");
                        })
                    .failureHandler(
                        (q, r, e) -> {
                          r.setStatus(401);
                          r.setContentType("application/json;charset=UTF-8");
                          r.getWriter().write("{\"message\":\"Sai tài khoản hoặc mật khẩu.\"}");
                        }))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((q, r, a) -> r.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (q, r, x) -> {
                          r.setStatus(401);
                          r.setContentType("application/json;charset=UTF-8");
                          r.getWriter().write("{\"message\":\"Vui lòng đăng nhập lại.\"}");
                        })
                    .accessDeniedHandler(
                        (q, r, x) -> {
                          r.setStatus(403);
                          r.setContentType("application/json;charset=UTF-8");
                          r.getWriter()
                              .write(
                                  "{\"message\":\"Không có quyền hoặc phiên bảo vệ đã hết hạn.\"}");
                        }))
        .build();
  }
}
