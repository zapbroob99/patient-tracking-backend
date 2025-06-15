package com.rhtsystem.randevuhastatakip.config;

import com.rhtsystem.randevuhastatakip.service.UserDetailsServiceImpl;
import com.rhtsystem.randevuhastatakip.service.UserService; // UserService'deki rol sabitleri için
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Spring Security'yi web uygulaması için etkinleştirir
@EnableMethodSecurity(prePostEnabled = true) // Metod seviyesinde güvenlik (@PreAuthorize vb.) için
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
            .csrf(csrf -> csrf.disable()) // Geliştirme için CSRF'i kapattık, production'da açılabilir.
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                        "/", // Ana sayfa
                        "/home", // Home yönlendirmesi için
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error" // Hata sayfaları için
                ).permitAll() // Bu path'lere herkes erişebilir

                // Örnek: Admin rolü için bir path (ileride gerekirse)
                // .requestMatchers("/admin/**").hasRole(UserService.ROLE_ADMIN_KISA_AD) // ROLE_ADMIN'in "ADMIN" kısmı

                // Diğer tüm istekler kimlik doğrulama gerektirir
                .anyRequest().authenticated()
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login") // Özel login sayfamızın yolu (Spring'in varsayılanı yerine)
                .permitAll() // Login sayfasına herkes erişebilir
                .defaultSuccessUrl("/home", true) // Başarılı giriş sonrası yönlendirilecek varsayılan URL
                .failureUrl("/login?error=true") // Başarısız giriş sonrası yönlendirilecek URL
            )
            .logout(logout -> logout
                .logoutUrl("/logout") // Logout işlemini tetikleyecek URL
                .logoutSuccessUrl("/login?logout") // Başarılı logout sonrası yönlendirilecek URL
                .invalidateHttpSession(true) // HTTP session'ı geçersiz kıl
                .deleteCookies("JSESSIONID") // Çerezleri sil (opsiyonel)
                .permitAll()
            );
            // .exceptionHandling(exceptions -> exceptions
            //     .accessDeniedPage("/access-denied") // Yetkisiz erişim durumunda yönlendirilecek sayfa (ileride oluşturulabilir)
            // );

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