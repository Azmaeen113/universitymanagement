package com.university.management.config;

import com.university.management.security.LoginFailureHandler;
import com.university.management.security.LoginSuccessHandler;
import com.university.management.security.RestAccessDeniedHandler;
import com.university.management.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    // Used to hash passwords on save and to check them at login
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Spring finds CustomUserDetailsService and the PasswordEncoder bean by itself.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // public pages
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/error").permitAll()

                // admin only
                .requestMatchers("/admin/**", "/api/admins/**").hasRole("ADMIN")

                // only an admin can create teachers; teachers edit/delete themselves (checked in service)
                .requestMatchers(HttpMethod.POST, "/api/teachers", "/api/teachers/**").hasRole("ADMIN")
                .requestMatchers("/teacher/**", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")

                // students API: approve is admin only, register is admin + teacher, rest is for all three roles
                .requestMatchers(HttpMethod.PUT, "/api/students/*/approve").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/students", "/api/students/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/student/**").hasAnyRole("ADMIN", "STUDENT")
                .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")

                // everything else needs a login
                .anyRequest().authenticated())

            // session based login page
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll())

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())

            // lets tools like Postman / curl call the REST API with email + password
            .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))

            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))

            // CSRF stays ON for all browser forms (Thymeleaf adds the token automatically).
            // It is skipped only when the caller sends an Authorization header (Basic auth),
            // because a browser never adds that header by itself, so CSRF is not possible there.
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                request -> request.getHeader("Authorization") != null));

        return http.build();
    }
}
