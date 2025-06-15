package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String rootPage(HttpServletRequest request, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Kullanıcı giriş yapmışsa, rolüne göre yönlendir
            if (request.isUserInRole(UserService.ROLE_ADMIN_PREFIXED)) { // UserService.ROLE_ADMIN_PREFIXED = "ROLE_ADMIN"
                return "redirect:/admin/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_DOKTOR_PREFIXED)) { // UserService.ROLE_DOKTOR_PREFIXED = "ROLE_DOKTOR"
                return "redirect:/doctor/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_HASTA_PREFIXED)) { // UserService.ROLE_HASTA_PREFIXED = "ROLE_HASTA"
                return "redirect:/patient/dashboard";
            }
            // Eğer bilinen bir role sahip değilse veya dashboard'u yoksa genel bir sayfaya.
            // Bu durum normalde olmamalı. Güvenlik açısından login'e yönlendirmek daha iyi olabilir.
            return "redirect:/home"; // veya "error/unknown-role" gibi bir sayfaya
        }
        // Giriş yapmamış kullanıcı için genel bir ana sayfa (opsiyonel) veya direkt login
        // model.addAttribute("welcomeMessage", "Randevu Sistemine Hoş Geldiniz!");
        // return "index"; // index.html (henüz yok)
        return "redirect:/login"; // Şimdilik direkt login'e yönlendiriyoruz
    }

    @GetMapping("/home")
    public String homePage(HttpServletRequest request) {
        // Bu sayfa, başarılı login sonrası defaultSuccessUrl ile gelinen yer.
        // Buradan da role göre yönlendirme yapalım.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (request.isUserInRole(UserService.ROLE_ADMIN_PREFIXED)) {
                return "redirect:/admin/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_DOKTOR_PREFIXED)) {
                return "redirect:/doctor/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_HASTA_PREFIXED)) {
                return "redirect:/patient/dashboard";
            }
            // Eğer yukarıdaki rollerden hiçbiri değilse (bu durum olmamalı)
            // ya da genel bir "giriş yapılmış kullanıcı" sayfası varsa oraya.
            // Şimdilik hata durumu olarak login'e atalım.
             return "redirect:/login?unknownrole"; // Veya "error/access-denied"
        }
        // Eğer bir şekilde buraya gelinmiş ve authentication yoksa (çok düşük ihtimal)
        return "redirect:/login";
    }
}