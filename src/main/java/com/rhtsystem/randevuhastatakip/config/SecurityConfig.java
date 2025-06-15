package com.rhtsystem.randevuhastatakip.config;

import com.rhtsystem.randevuhastatakip.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Spring Security'yi web uygulaması için etkinleştirir
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Şimdilik CSRF'i kapatalım, test için. Sonra açabiliriz.
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/public/**", "/auth/**", "/register", "/login", "/css/**", "/js/**", "/images/**").permitAll() // Bu path'lere herkes erişebilir
                // .requestMatchers("/doktor/**").hasRole("DOKTOR") // Örnek rol bazlı yetkilendirme
                // .requestMatchers("/hasta/**").hasRole("HASTA")   // Örnek rol bazlı yetkilendirme
                .anyRequest().authenticated() // Diğer tüm istekler kimlik doğrulama gerektirir
            )
            .formLogin(formLogin -> formLogin
                // .loginPage("/login") // Kendi özel login sayfamız olursa
                .permitAll() // Login sayfasına herkes erişebilir
                // .defaultSuccessUrl("/home", true) // Başarılı giriş sonrası yönlendirme
            )
            .logout(logout -> logout
                .permitAll()
                // .logoutSuccessUrl("/login?logout") // Başarılı logout sonrası yönlendirme
            );
            // .userDetailsService(userDetailsService); // AuthenticationManagerBuilder ile konfigüre edilecek

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                                    .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }
}