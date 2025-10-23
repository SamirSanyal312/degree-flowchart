package com.example.degree_planner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/**").authenticated()      // API requires login
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/flow", true)                // always go to /flow after login
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
            );
        // CSRF remains enabled (default) – good for form login and we’ll send the token with API POSTs.
        return http.build();
    }

    // Demo users. Replace with a DB-backed UserDetailsService when ready.
    @Bean
    public UserDetailsService userDetailsService() {
        var u1 = User.withUsername("student1").password("{noop}pass").roles("STUDENT").build();
        var u2 = User.withUsername("student2").password("{noop}pass").roles("STUDENT").build();
        return new InMemoryUserDetailsManager(u1, u2);
    }
}
