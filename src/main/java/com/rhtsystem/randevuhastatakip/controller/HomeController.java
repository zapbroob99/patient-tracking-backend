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

    // ROLE_DOKTOR için UserService'deki sabiti kullandığınızdan emin olun
    // public static final String ROLE_DOKTOR = "ROLE_DOKTOR"; (UserService'de)

    @GetMapping("/")
    public String rootRedirect(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (request.isUserInRole(UserService.ROLE_HASTA)) {
                return "redirect:/patient/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_DOKTOR)) { // DOKTOR ROLÜ KONTROLÜ
                return "redirect:/doctor/dashboard"; // DOCTOR DASHBOARD'A YÖNLENDİR
            }
            return "redirect:/home";
        }
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String homePage(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (request.isUserInRole(UserService.ROLE_HASTA)) {
                return "redirect:/patient/dashboard";
            } else if (request.isUserInRole(UserService.ROLE_DOKTOR)) { // DOKTOR ROLÜ KONTROLÜ
                return "redirect:/doctor/dashboard"; // DOCTOR DASHBOARD'A YÖNLENDİR
            }
             return "redirect:/login?unknownrole";
        }
        return "redirect:/login";
    }
}