package com.rhtsystem.randevuhastatakip.controller;

import com.rhtsystem.randevuhastatakip.dto.UserRegistrationDto;
import com.rhtsystem.randevuhastatakip.model.User;
import com.rhtsystem.randevuhastatakip.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
// import org.springframework.validation.BindingResult; // Validasyon için
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
// import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Model model, String error, String logout, String registered, String successMessage) {
        // Bu parametreler SecurityConfig'den veya redirect'lerden gelebilir.
        // Thymeleaf'te ${param.error}, ${param.logout}, ${param.registered} ile yakalanabilir.
        // Flash attribute olarak gelen successMessage'ı da model'e ekleyelim.
        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
        }
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            // @Valid
            @ModelAttribute("userDto") UserRegistrationDto userDto,
            // BindingResult result, // Validasyon için
            Model model,
            RedirectAttributes redirectAttributes) {

        // if (result.hasErrors()) { // Validasyon için
        //     model.addAttribute("userDto", userDto);
        //     return "register";
        // }

        if (userDto.getPassword() == null || userDto.getConfirmPassword() == null || !userDto.getPassword().equals(userDto.getConfirmPassword())) {
            model.addAttribute("userDto", userDto);
            model.addAttribute("passwordError", "Şifreler uyuşmuyor veya boş bırakılmış!");
            return "register";
        }

        try {
            User newUser = new User();
            newUser.setUsername(userDto.getUsername());
            newUser.setPassword(userDto.getPassword());
            newUser.setFirstName(userDto.getFirstName());
            newUser.setLastName(userDto.getLastName());

            // Genel kayıt sadece HASTA rolü için olacak.
            // UserService.registerNewUser metoduna ROLE_HASTA_PREFIXED gönderiyoruz.
            userService.registerNewUser(newUser, UserService.ROLE_HASTA_PREFIXED, Optional.empty());

            redirectAttributes.addFlashAttribute("successMessage", "Kayıt başarılı! Lütfen giriş yapınız.");
            return "redirect:/login"; // ?registered parametresini kaldırdık, successMessage flash attribute ile taşınıyor

        } catch (Exception e) {
            model.addAttribute("userDto", userDto);
            model.addAttribute("registrationError", e.getMessage());
            return "register";
        }
    }

    // Yetkisiz erişim durumunda gösterilecek sayfa
    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "error/access-denied"; // error/access-denied.html
    }
}