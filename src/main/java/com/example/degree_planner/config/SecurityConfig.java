package com.example.degree_planner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**",
                         "/favicon.ico", "/login", "/error").permitAll()
        .anyRequest().authenticated()
      )
      .formLogin(login -> login
        .loginPage("/login").permitAll()
        .defaultSuccessUrl("/flow", true)
      )
      // Allow GET /logout because the UI uses a link
      .logout(logout -> logout
        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
        .logoutSuccessUrl("/login?logout")
        .clearAuthentication(true)
        .invalidateHttpSession(true)
        .deleteCookies("JSESSIONID")
        .permitAll()
      )
      // Keep CSRF enabled, but ignore logout(GET) and API endpoints used by AJAX
      .csrf(csrf -> csrf.ignoringRequestMatchers(
          new AntPathRequestMatcher("/logout", "GET"),
          new AntPathRequestMatcher("/api/**")
      ))
      .headers(Customizer.withDefaults());

    return http.build();
  }

  /* If you already have a UserDetailsService via JDBC, remove this block. */
  @Bean
  public UserDetailsService users(PasswordEncoder encoder) {
    UserDetails s1 = User.withUsername("student1").password(encoder.encode("pass")).roles("USER").build();
    UserDetails s2 = User.withUsername("student2").password(encoder.encode("pass")).roles("USER").build();
    return new InMemoryUserDetailsManager(s1, s2);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
