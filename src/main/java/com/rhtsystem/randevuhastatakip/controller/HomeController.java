package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String rootRedirect(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
             // Eğer kullanıcı giriş yapmışsa rollerine göre yönlendir
            if (request.isUserInRole(UserService.ROLE_HASTA)) { // ROLE_HASTA direkt kullanılabilir
                return "redirect:/patient/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_DOKTOR)) {
                return "redirect:/doctor/dashboard"; // Henüz yok ama ileride olacak
            }
            return "redirect:/home"; // Varsayılan giriş yapmış kullanıcı sayfası
        }
        return "redirect:/login"; // Giriş yapmamışsa login'e
    }

    @GetMapping("/home")
    public String homePage(HttpServletRequest request) {
        // Kullanıcının rolüne göre doğru dashboard'a yönlendirme yapabiliriz
        // Veya genel bir "hoş geldiniz" sayfası gösterebiliriz.
        // Şimdilik kök dizindeki mantığı tekrar edelim:
        if (request.isUserInRole(UserService.ROLE_HASTA)) {
            return "redirect:/patient/dashboard";
        } else if (request.isUserInRole(UserService.ROLE_DOKTOR)) {
            return "redirect:/doctor/dashboard";
        }
        // Eğer hiçbir role uymuyorsa veya rol bazlı dashboard yoksa genel bir sayfa
        // return "home"; // home.html (Basit bir hoş geldin sayfası olabilir)
        return "redirect:/login"; // veya login'e at
    }
}