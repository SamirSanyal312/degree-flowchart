package com.example.degree_planner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  // 1) Password encoder (BCrypt)
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // 2) Two in-memory users for testing
  @Bean
  public UserDetailsService users(PasswordEncoder encoder) {
    UserDetails student1 = User.withUsername("student1")
        .password(encoder.encode("pass1"))
        .roles("STUDENT")
        .build();

    UserDetails student2 = User.withUsername("student2")
        .password(encoder.encode("pass2"))
        .roles("STUDENT")
        .build();

    return new InMemoryUserDetailsManager(student1, student2);
  }

  // 3) Basic security rules + default login form
  @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .authorizeHttpRequests(auth -> auth
      .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
      .anyRequest().authenticated()
    )
    .formLogin(form -> form
      // if you have templates/login.html, keep this line:
      .loginPage("/login").permitAll()
      // where to go after a successful login:
      .defaultSuccessUrl("/courses", true)   // change to "/flow" if you prefer
    )
    .logout(logout -> logout
      .logoutUrl("/logout")
      .logoutSuccessUrl("/login?logout").permitAll()
    );

  return http.build();
}
}